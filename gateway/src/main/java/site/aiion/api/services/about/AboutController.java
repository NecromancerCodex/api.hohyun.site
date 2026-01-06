package site.aiion.api.services.about;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import site.aiion.api.services.about.common.domain.Messenger;
import site.aiion.api.services.diary.util.JwtTokenUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/about")
@Tag(name = "About", description = "자기소개글 관리 기능")
public class AboutController {

    private final AboutService aboutService;
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping("/user")
    @Operation(summary = "자기소개글 조회 (Public)", description = "userId 1의 자기소개글을 조회합니다. 인증 불필요 (게스트 포함 모두 조회 가능).")
    public Messenger findByUserIdFromToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 인증 여부와 관계없이 userId 1의 자기소개글을 조회 (Public 엔드포인트)
        // 게스트, 로그인 사용자 모두 조회 가능
        return aboutService.findByUserId(1L);
    }

    @PostMapping
    @Operation(summary = "자기소개글 저장", description = "새로운 자기소개글을 저장합니다. userId 1만 권한이 있습니다.")
    public Messenger save(
            @RequestBody AboutModel aboutModel,
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

        // userId 1만 자기소개글 작성 권한 있음
        if (!tokenUserId.equals(1L)) {
            return Messenger.builder()
                    .code(403)
                    .message("자기소개글 작성 권한이 없습니다. (userId 1만 가능)")
                    .build();
        }

        aboutModel.setUserId(tokenUserId);
        return aboutService.save(aboutModel);
    }

    @PutMapping
    @Operation(summary = "자기소개글 수정", description = "기존 자기소개글을 수정합니다. userId 1만 권한이 있습니다.")
    public Messenger update(
            @RequestBody AboutModel aboutModel,
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

        // userId 1만 자기소개글 수정 권한 있음
        if (!tokenUserId.equals(1L)) {
            return Messenger.builder()
                    .code(403)
                    .message("자기소개글 수정 권한이 없습니다. (userId 1만 가능)")
                    .build();
        }

        aboutModel.setUserId(tokenUserId);
        return aboutService.update(aboutModel);
    }

    @DeleteMapping
    @Operation(summary = "자기소개글 삭제", description = "자기소개글을 삭제합니다. userId 1만 권한이 있습니다.")
    public Messenger delete(
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

        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Messenger.builder()
                    .code(401)
                    .message("토큰에서 사용자 ID를 추출할 수 없습니다.")
                    .build();
        }

        // userId 1만 자기소개글 삭제 권한 있음
        if (!userId.equals(1L)) {
            return Messenger.builder()
                    .code(403)
                    .message("자기소개글 삭제 권한이 없습니다. (userId 1만 가능)")
                    .build();
        }

        return aboutService.delete(userId);
    }
}

