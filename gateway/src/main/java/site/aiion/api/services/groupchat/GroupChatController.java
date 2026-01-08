package site.aiion.api.services.groupchat;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.aiion.api.services.about.common.domain.Messenger;
import site.aiion.api.services.diary.util.JwtTokenUtil;
import site.aiion.api.services.user.UserRepository;
import site.aiion.api.services.user.User;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groupchat")
@Tag(name = "GroupChat", description = "단체 채팅방 기능")
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "메시지 전송", description = "단체 채팅방에 메시지를 전송합니다. 인증된 사용자만 가능합니다.")
    public Messenger sendMessage(
            @RequestBody GroupChatModel groupChatModel,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // JWT 토큰에서 userId 추출 및 검증
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Messenger.builder()
                    .code(401)
                    .message("인증 토큰이 필요합니다. 로그인 후 메시지를 보낼 수 있습니다.")
                    .build();
        }

        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return Messenger.builder()
                    .code(401)
                    .message("유효하지 않은 토큰입니다.")
                    .build();
        }

        Long tokenUserId = jwtTokenUtil.getUserIdFromToken(token);
        if (tokenUserId == null) {
            return Messenger.builder()
                    .code(401)
                    .message("토큰에서 사용자 ID를 추출할 수 없습니다.")
                    .build();
        }

        // 토큰의 userId로 설정 (클라이언트에서 보낸 userId는 무시)
        groupChatModel.setUserId(tokenUserId);
        
        // User 정보에서 nickname 가져와서 username에 설정
        Optional<User> userOpt = userRepository.findById(tokenUserId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // nickname이 있으면 nickname 사용, 없으면 name 사용
            String username = user.getNickname() != null && !user.getNickname().isEmpty()
                    ? user.getNickname()
                    : (user.getName() != null ? user.getName() : "사용자 " + tokenUserId);
            groupChatModel.setUsername(username);
        } else {
            groupChatModel.setUsername("사용자 " + tokenUserId);
        }
        
        return groupChatService.save(groupChatModel);
    }

    @GetMapping
    @Operation(summary = "메시지 목록 조회 (Public)", description = "단체 채팅방 메시지 목록을 조회합니다. 인증 불필요 (모두 조회 가능).")
    public Messenger getMessages(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return groupChatService.findAll(pageable);
    }

    @GetMapping("/recent")
    @Operation(summary = "최근 메시지 조회 (Public)", description = "최근 N개의 메시지를 조회합니다. 인증 불필요 (실시간 채팅용).")
    public Messenger getRecentMessages(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        
        return groupChatService.findRecentMessages(limit);
    }

    @DeleteMapping("/all")
    @Operation(summary = "모든 메시지 삭제", description = "단체 채팅방의 모든 메시지를 삭제합니다. userId 1만 권한이 있습니다.")
    public Messenger deleteAllMessages(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // JWT 토큰에서 userId 추출 및 검증
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Messenger.builder()
                    .code(401)
                    .message("인증 토큰이 필요합니다.")
                    .build();
        }

        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return Messenger.builder()
                    .code(401)
                    .message("유효하지 않은 토큰입니다.")
                    .build();
        }

        Long tokenUserId = jwtTokenUtil.getUserIdFromToken(token);
        if (tokenUserId == null) {
            return Messenger.builder()
                    .code(401)
                    .message("토큰에서 사용자 ID를 추출할 수 없습니다.")
                    .build();
        }

        // userId 1만 권한 허용
        if (!tokenUserId.equals(1L)) {
            return Messenger.builder()
                    .code(403)
                    .message("메시지 삭제 권한이 없습니다. (userId 1만 가능)")
                    .build();
        }

        return groupChatService.deleteAll();
    }
}

