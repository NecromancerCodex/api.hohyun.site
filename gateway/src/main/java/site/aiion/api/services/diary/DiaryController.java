package site.aiion.api.services.diary;

import java.util.List;

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
import site.aiion.api.services.diary.common.domain.Messenger;
import site.aiion.api.services.diary.util.JwtTokenUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diaries")
@Tag(name = "Diary", description = "일기 관리 기능")
public class DiaryController {

    private final DiaryService diaryService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/findById")
    @Operation(summary = "일기 ID로 조회 (공개)", description = "일기 ID를 받아 해당 일기 정보를 조회합니다. 인증 없이 모든 사용자가 조회 가능합니다.")
    public Messenger findById(@RequestBody DiaryModel diaryModel) {
        return diaryService.findById(diaryModel);
    }

    @GetMapping
    @Operation(summary = "전체 일기 조회 (공개)", description = "모든 일기 정보를 조회합니다. 인증 없이 모든 사용자가 조회 가능합니다.")
    public Messenger findAll() {
        return diaryService.findAll();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자별 일기 조회 (공개)", description = "특정 사용자의 일기 정보를 조회합니다. 인증 없이 모든 사용자가 조회 가능합니다.")
    public Messenger findByUserId(
            @org.springframework.web.bind.annotation.PathVariable Long userId) {
        return diaryService.findByUserId(userId);
    }
    
    @GetMapping("/user")
    @Operation(summary = "JWT 토큰 기반 일기 조회 (공개)", description = "JWT 토큰에서 사용자 ID를 추출하여 해당 사용자의 일기 정보를 조회합니다. 토큰이 없어도 전체 일기를 조회할 수 있습니다.")
    public Messenger findByUserIdFromToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 토큰이 있으면 해당 사용자의 일기만 조회
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtTokenUtil.validateToken(token)) {
                Long userId = jwtTokenUtil.getUserIdFromToken(token);
                if (userId != null) {
                    return diaryService.findByUserId(userId);
                }
            }
        }
        // 토큰이 없으면 전체 일기 조회
        return diaryService.findAll();
    }

    @GetMapping("/check/{userId}")
    @Operation(summary = "사용자별 일기 연결 확인", description = "특정 사용자의 일기 연결 상태를 확인합니다.")
    public Messenger checkUserDiaryConnection(@org.springframework.web.bind.annotation.PathVariable Long userId) {
        return diaryService.findByUserId(userId);
    }

    @PostMapping
    @Operation(summary = "일기 저장", description = "새로운 일기 정보를 저장합니다. userId 1만 권한이 있습니다.")
    public Messenger save(
            @RequestBody DiaryModel diaryModel,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰 검증
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
                    .message("일기 작성 권한이 없습니다. userId 1만 작성할 수 있습니다.")
                    .build();
        }
        
        // 토큰의 userId로 설정
        diaryModel.setUserId(tokenUserId);
        
        System.out.println("[DiaryController] 저장 요청 수신 (userId=" + tokenUserId + "):");
        System.out.println("  - id: " + diaryModel.getId());
        System.out.println("  - diaryDate: " + diaryModel.getDiaryDate());
        System.out.println("  - title: " + diaryModel.getTitle());
        System.out.println("  - content: " + (diaryModel.getContent() != null ? diaryModel.getContent().length() + "자" : "null"));
        
        return diaryService.save(diaryModel);
    }

    @PostMapping("/saveAll")
    @Operation(summary = "일기 일괄 저장", description = "여러 일기 정보를 한 번에 저장합니다. userId 1만 권한이 있습니다.")
    public Messenger saveAll(
            @RequestBody List<DiaryModel> diaryModelList,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰 검증
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
                    .message("일기 일괄 저장 권한이 없습니다. userId 1만 저장할 수 있습니다.")
                    .build();
        }
        
        // 모든 일기에 userId 설정
        diaryModelList.forEach(model -> model.setUserId(tokenUserId));
        
        return diaryService.saveAll(diaryModelList);
    }

    @PutMapping
    @Operation(summary = "일기 수정", description = "기존 일기 정보를 수정합니다. userId 1만 권한이 있습니다.")
    public Messenger update(
            @RequestBody DiaryModel diaryModel,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰 검증
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
                    .message("일기 수정 권한이 없습니다. userId 1만 수정할 수 있습니다.")
                    .build();
        }
        
        // 토큰의 userId로 설정
        diaryModel.setUserId(tokenUserId);
        
        return diaryService.update(diaryModel);
    }

    @DeleteMapping
    @Operation(summary = "일기 삭제", description = "일기 정보를 삭제합니다. userId 1만 권한이 있습니다.")
    public Messenger delete(
            @RequestBody DiaryModel diaryModel,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰 검증
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
                    .message("일기 삭제 권한이 없습니다. userId 1만 삭제할 수 있습니다.")
                    .build();
        }
        
        // 토큰의 userId로 설정
        diaryModel.setUserId(tokenUserId);
        
        System.out.println("[DiaryController] 삭제 요청 수신 (userId=" + tokenUserId + "):");
        System.out.println("  - id: " + diaryModel.getId());
        
        Messenger result = diaryService.delete(diaryModel);
        System.out.println("[DiaryController] 삭제 결과: Code=" + result.getCode() + ", message=" + result.getMessage());
        return result;
    }

    @PostMapping("/reanalyze-emotions/{userId}")
    @Operation(summary = "기존 일기 감정 분석 재실행 (수동)", description = "모델 재학습 후 기존 일기들을 새 모델로 재분석합니다. 수동 실행용입니다.")
    public Messenger reanalyzeEmotionsForUser(
            @org.springframework.web.bind.annotation.PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰 검증 (선택사항)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtTokenUtil.validateToken(token)) {
                Long tokenUserId = jwtTokenUtil.getUserIdFromToken(token);
                if (tokenUserId != null && !tokenUserId.equals(userId)) {
                    return Messenger.builder()
                            .code(403)
                            .message("권한이 없습니다. 자신의 일기만 재분석할 수 있습니다.")
                            .build();
                }
            }
        }
        
        System.out.println("[DiaryController] 사용자 ID " + userId + "의 기존 일기 감정 분석 재실행 시작");
        Messenger result = diaryService.reanalyzeEmotionsForUser(userId);
        System.out.println("[DiaryController] 감정 분석 재실행 결과: " + result.getMessage());
        return result;
    }

    @PostMapping("/reanalyze-all-emotions")
    @Operation(summary = "모든 일기 감정 분석 (수동)", description = "일기 테이블의 모든 일기를 새 모델로 분석합니다. 수동 실행용입니다.")
    public Messenger reanalyzeAllEmotions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("[DiaryController] 모든 일기 감정 분석 시작");
        Messenger result = diaryService.reanalyzeAllEmotions();
        System.out.println("[DiaryController] 전체 감정 분석 결과: " + result.getMessage());
        return result;
    }

    @PostMapping("/reanalyze-mbti/{userId}")
    @Operation(summary = "기존 일기 MBTI 분석 재실행 (수동)", description = "모델 재학습 후 기존 일기들을 새 모델로 재분석합니다. 수동 실행용입니다.")
    public Messenger reanalyzeMbtiForUser(
            @org.springframework.web.bind.annotation.PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // JWT 토큰 검증 (선택사항)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtTokenUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtTokenUtil.validateToken(token)) {
                Long tokenUserId = jwtTokenUtil.getUserIdFromToken(token);
                if (tokenUserId != null && !tokenUserId.equals(userId)) {
                    return Messenger.builder()
                            .code(403)
                            .message("권한이 없습니다. 자신의 일기만 재분석할 수 있습니다.")
                            .build();
                }
            }
        }
        
        System.out.println("[DiaryController] 사용자 ID " + userId + "의 기존 일기 MBTI 분석 재실행 시작");
        Messenger result = diaryService.reanalyzeMbtiForUser(userId);
        System.out.println("[DiaryController] MBTI 분석 재실행 결과: " + result.getMessage());
        return result;
    }

    @PostMapping("/reanalyze-all-mbti")
    @Operation(summary = "모든 일기 MBTI 분석 (수동)", description = "일기 테이블의 모든 일기를 새 모델로 분석합니다. 수동 실행용입니다.")
    public Messenger reanalyzeAllMbti(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        System.out.println("[DiaryController] 모든 일기 MBTI 분석 시작");
        Messenger result = diaryService.reanalyzeAllMbti();
        System.out.println("[DiaryController] 전체 MBTI 분석 결과: " + result.getMessage());
        return result;
    }

}

