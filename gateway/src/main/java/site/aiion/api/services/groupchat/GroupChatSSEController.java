package site.aiion.api.services.groupchat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "GroupChat SSE", description = "단체 채팅방 실시간 스트리밍")
public class GroupChatSSEController {

    private final GroupChatRepository groupChatRepository;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastMessageIds = new ConcurrentHashMap<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "실시간 메시지 스트림 (SSE)", description = "단체 채팅방의 새로운 메시지를 실시간으로 받습니다. 인증 불필요 (Public).")
    public SseEmitter streamMessages(
            @RequestParam(value = "lastId", defaultValue = "0") Long lastId) {
        
        log.info("SSE 연결 시작: lastId={}", lastId);
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 타임아웃
        String emitterId = String.valueOf(System.currentTimeMillis());
        emitters.put(emitterId, emitter);
        lastMessageIds.put(emitterId, new AtomicLong(lastId));

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

        // 초기 메시지 전송
        sendInitialMessages(emitter, lastId);

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
            for (GroupChatModel msg : recentMessages) {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(msg.getId()))
                        .name("message")
                        .data(msg));
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
}

