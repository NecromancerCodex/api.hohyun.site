package site.aiion.api.services.groupchat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groupchat")
@org.springframework.context.annotation.Scope("singleton")
@Tag(name = "GroupChat SSE", description = "단체 채팅방 실시간 스트리밍")
public class GroupChatSSEController {

    private final GroupChatRepository groupChatRepository;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastMessageIds = new ConcurrentHashMap<>();

    @CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS})
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 메시지 스트림 (SSE)", description = "단체 채팅방의 새로운 메시지를 실시간으로 받습니다. 인증 불필요 (Public).")
    public SseEmitter streamMessages(
            @RequestParam(value = "lastId", defaultValue = "0") Long lastId,
            HttpServletResponse response) {
        
        // SSE 필수 헤더 설정
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");  // Nginx 버퍼링 비활성화
        
        log.info("========== SSE 연결 요청 ==========");
        log.info("lastId: {}", lastId);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 타임아웃
        String emitterId = String.valueOf(System.currentTimeMillis());
        emitters.put(emitterId, emitter);
        lastMessageIds.put(emitterId, new AtomicLong(lastId));
        
        log.info("SSE Emitter 생성: emitterId={}, 현재 연결 수={}", emitterId, emitters.size());

        // 연결 종료 시 정리
        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료: emitterId={}", emitterId);
            emitters.remove(emitterId);
            lastMessageIds.remove(emitterId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃: emitterId={}", emitterId);
            emitters.remove(emitterId);
            lastMessageIds.remove(emitterId);
        });

        emitter.onError((ex) -> {
            log.error("SSE 연결 오류: emitterId={}", emitterId, ex);
            emitters.remove(emitterId);
            lastMessageIds.remove(emitterId);
        });

        try {
            // 연결 확립을 위한 초기 이벤트 전송 (필수!)
            log.info("초기 연결 이벤트 전송 중...");
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
            log.info("✓ 초기 연결 이벤트 전송 성공");
            
            // 초기 메시지 전송
            sendInitialMessages(emitter, lastId);
        } catch (IOException e) {
            log.error("초기 이벤트 전송 실패: emitterId={}", emitterId, e);
            emitters.remove(emitterId);
            lastMessageIds.remove(emitterId);
            emitter.completeWithError(e);
            return emitter;
        }

        // 주기적으로 새 메시지 확인 (1초마다)
        executor.scheduleAtFixedRate(() -> {
            try {
                if (!emitters.containsKey(emitterId)) {
                    return; // 연결이 종료된 경우
                }

                AtomicLong currentLastId = lastMessageIds.get(emitterId);
                if (currentLastId == null) {
                    return;
                }

                List<GroupChatModel> newMessages = getMessagesAfterId(currentLastId.get());
                
                if (!newMessages.isEmpty()) {
                    for (GroupChatModel msg : newMessages) {
                        if (msg.getId() != null) {
                            currentLastId.set(msg.getId());
                        }
                        try {
                            emitter.send(SseEmitter.event()
                                    .id(String.valueOf(msg.getId()))
                                    .name("message")
                                    .data(msg));
                        } catch (IOException e) {
                            log.error("SSE 메시지 전송 오류", e);
                            emitters.remove(emitterId);
                            lastMessageIds.remove(emitterId);
                            return;
                        }
                    }
                } else {
                    // keep-alive
                    try {
                        emitter.send(SseEmitter.event()
                                .name("ping")
                                .comment("keep-alive"));
                    } catch (IOException e) {
                        log.error("SSE keep-alive 전송 오류", e);
                        emitters.remove(emitterId);
                        lastMessageIds.remove(emitterId);
                    }
                }
            } catch (Exception e) {
                log.error("SSE 스케줄러 오류", e);
                emitters.remove(emitterId);
                lastMessageIds.remove(emitterId);
            }
        }, 1, 1, TimeUnit.SECONDS);

        return emitter;
    }

    private void sendInitialMessages(SseEmitter emitter, Long lastId) {
        try {
            List<GroupChatModel> recentMessages = getMessagesAfterId(lastId);
            log.info("초기 메시지 개수: {}", recentMessages.size());
            
            if (recentMessages.isEmpty()) {
                log.info("초기 메시지 없음 (정상)");
                return;
            }
            
            for (GroupChatModel msg : recentMessages) {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(msg.getId()))
                        .name("message")
                        .data(msg));
                log.info("초기 메시지 전송: id={}", msg.getId());
            }
        } catch (Exception e) {
            log.error("초기 메시지 전송 오류", e);
        }
    }

    private List<GroupChatModel> getMessagesAfterId(Long lastId) {
        try {
            List<GroupChat> entities = groupChatRepository.findAll();
            
            // lastId 이후의 메시지만 필터링
            List<GroupChat> filteredEntities = entities.stream()
                    .filter(entity -> {
                        if (entity.getId() == null) return false;
                        return entity.getId() > lastId;
                    })
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    })
                    .collect(Collectors.toList());
            
            return filteredEntities.stream()
                    .map(this::entityToModel)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("메시지 조회 오류", e);
            return List.of();
        }
    }

    private GroupChatModel entityToModel(GroupChat entity) {
        if (entity == null) {
            return null;
        }
        return GroupChatModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    /**
     * 모든 연결된 클라이언트에게 새 메시지 브로드캐스트
     */
    public void broadcastMessage(GroupChatModel message) {
        if (message == null || message.getId() == null) {
            log.warn("브로드캐스트 실패: 메시지가 null이거나 ID가 없음");
            return;
        }
        
        log.info("====== 브로드캐스트 시작 ======");
        log.info("메시지 ID: {}, 내용: {}, 사용자: {}", message.getId(), message.getMessage(), message.getUsername());
        log.info("연결된 클라이언트 수: {}", emitters.size());
        log.info("===========================");
        
        if (emitters.isEmpty()) {
            log.warn("연결된 SSE 클라이언트가 없습니다!");
            return;
        }
        
        List<String> toRemove = new java.util.ArrayList<>();
        int successCount = 0;
        int skipCount = 0;
        
        for (java.util.Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            AtomicLong currentLastId = lastMessageIds.get(emitterId);
            
            try {
                log.info("클라이언트 {}: lastId={}, 새 메시지 ID={}", 
                    emitterId, 
                    currentLastId != null ? currentLastId.get() : "null", 
                    message.getId());
                
                // 현재 클라이언트의 lastId보다 큰 메시지만 전송
                if (currentLastId != null && message.getId() > currentLastId.get()) {
                    currentLastId.set(message.getId());
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(message.getId()))
                            .name("message")
                            .data(message));
                    successCount++;
                    log.info("✓ SSE 메시지 전송 성공: emitterId={}, messageId={}", emitterId, message.getId());
                } else {
                    skipCount++;
                    log.info("⊘ SSE 메시지 스킵 (이미 전송됨): emitterId={}, currentLastId={}, messageId={}", 
                        emitterId, 
                        currentLastId != null ? currentLastId.get() : "null", 
                        message.getId());
                }
            } catch (IOException e) {
                log.error("✗ SSE 브로드캐스트 오류: emitterId={}", emitterId, e);
                toRemove.add(emitterId);
            }
        }
        
        log.info("====== 브로드캐스트 완료 ======");
        log.info("성공: {}, 스킵: {}, 실패: {}", successCount, skipCount, toRemove.size());
        log.info("===========================");
        
        // 오류 발생한 연결 제거
        for (String emitterId : toRemove) {
            emitters.remove(emitterId);
            lastMessageIds.remove(emitterId);
            log.info("SSE 연결 제거: emitterId={}", emitterId);
        }
    }
}

