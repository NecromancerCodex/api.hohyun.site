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
    @Operation(summary = "JWT 토큰 기반 자기소개글 조회", description = "JWT 토큰에서 사용자 ID를 추출하여 해당 사용자의 자기소개글을 조회합니다.")
    public Messenger findByUserIdFromToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Authorization 헤더 검증
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Messenger.builder()
                    .code(401)
                    .message("인증 토큰이 필요합니다.")
                    .build();
        }

        // 토큰 추출 및 검증
        String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtTokenUtil.validateToken(token)) {
            return Messenger.builder()
                    .code(401)
                    .message("유효하지 않은 토큰입니다.")
                    .build();
        }

        // 토큰에서 userId 추출
        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        if (userId == null) {
            return Messenger.builder()
                    .code(401)
                    .message("토큰에서 사용자 ID를 추출할 수 없습니다.")
                    .build();
        }

        return aboutService.findByUserId(userId);
    }

    @PostMapping
    @Operation(summary = "자기소개글 저장", description = "새로운 자기소개글을 저장합니다. JWT 토큰에서 userId를 자동으로 추출합니다.")
    public Messenger save(
            @RequestBody AboutModel aboutModel,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰에서 userId 추출 및 설정
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtTokenUtil.validateToken(token)) {
                Long tokenUserId = jwtTokenUtil.getUserIdFromToken(token);
                if (tokenUserId != null) {
                    // 토큰의 userId로 덮어쓰기 (보안 강화)
                    aboutModel.setUserId(tokenUserId);
                }
            }
        }

        if (aboutModel.getUserId() == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        return aboutService.save(aboutModel);
    }

    @PutMapping
    @Operation(summary = "자기소개글 수정", description = "기존 자기소개글을 수정합니다. JWT 토큰에서 userId를 자동으로 추출합니다.")
    public Messenger update(
            @RequestBody AboutModel aboutModel,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰에서 userId 추출 및 설정
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtTokenUtil.validateToken(token)) {
                Long tokenUserId = jwtTokenUtil.getUserIdFromToken(token);
                if (tokenUserId != null) {
                    // 토큰의 userId로 덮어쓰기 (보안 강화)
                    aboutModel.setUserId(tokenUserId);
                }
            }
        }

        if (aboutModel.getUserId() == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        return aboutService.update(aboutModel);
    }

    @DeleteMapping
    @Operation(summary = "자기소개글 삭제", description = "자기소개글을 삭제합니다. JWT 토큰에서 userId를 자동으로 추출합니다.")
    public Messenger delete(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰에서 userId 추출
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

        return aboutService.delete(userId);
    }
}

