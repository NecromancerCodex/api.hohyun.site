package site.aiion.api.services.about;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import site.aiion.api.services.about.common.domain.Messenger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AboutServiceImpl implements AboutService {

    private final AboutRepository aboutRepository;

    private AboutModel entityToModel(About entity) {
        if (entity == null) {
            return null;
        }
        return AboutModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private About modelToEntity(AboutModel model) {
        return About.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .content(model.getContent())
                .build();
    }

    @Override
    public Messenger findByUserId(Long userId) {
        if (userId == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        try {
            Optional<About> entityOpt = aboutRepository.findByUserId(userId);
            if (entityOpt.isPresent()) {
                About entity = entityOpt.get();
                AboutModel model = entityToModel(entity);
                return Messenger.builder()
                        .code(200)
                        .message("자기소개글 조회 성공")
                        .data(model)
                        .build();
            } else {
                return Messenger.builder()
                        .code(404)
                        .message("자기소개글을 찾을 수 없습니다.")
                        .build();
            }
        } catch (Exception e) {
            log.error("자기소개글 조회 중 오류 발생: userId={}", userId, e);
            return Messenger.builder()
                    .code(500)
                    .message("자기소개글 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public Messenger save(AboutModel aboutModel) {
        if (aboutModel == null) {
            return Messenger.builder()
                    .code(400)
                    .message("자기소개글 정보가 필요합니다.")
                    .build();
        }

        if (aboutModel.getUserId() == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        try {
            // 이미 존재하는지 확인
            if (aboutRepository.existsByUserId(aboutModel.getUserId())) {
                return Messenger.builder()
                        .code(409)
                        .message("이미 자기소개글이 존재합니다. 수정 기능을 사용해주세요.")
                        .build();
            }

            About entity = modelToEntity(aboutModel);
            About savedEntity = aboutRepository.save(entity);
            AboutModel savedModel = entityToModel(savedEntity);

            log.info("자기소개글 저장 성공: userId={}, id={}", aboutModel.getUserId(), savedEntity.getId());
            return Messenger.builder()
                    .code(200)
                    .message("자기소개글 저장 성공")
                    .data(savedModel)
                    .build();
        } catch (Exception e) {
            log.error("자기소개글 저장 중 오류 발생: userId={}", aboutModel.getUserId(), e);
            return Messenger.builder()
                    .code(500)
                    .message("자기소개글 저장 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public Messenger update(AboutModel aboutModel) {
        if (aboutModel == null) {
            return Messenger.builder()
                    .code(400)
                    .message("자기소개글 정보가 필요합니다.")
                    .build();
        }

        if (aboutModel.getUserId() == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        try {
            Optional<About> existingEntity = aboutRepository.findByUserId(aboutModel.getUserId());
            if (existingEntity.isEmpty()) {
                return Messenger.builder()
                        .code(404)
                        .message("자기소개글을 찾을 수 없습니다. 먼저 저장해주세요.")
                        .build();
            }

            About entity = existingEntity.get();
            // 내용만 업데이트
            entity.setContent(aboutModel.getContent());
            // updatedAt은 @PreUpdate로 자동 설정됨

            About updatedEntity = aboutRepository.save(entity);
            AboutModel updatedModel = entityToModel(updatedEntity);

            log.info("자기소개글 수정 성공: userId={}, id={}", aboutModel.getUserId(), updatedEntity.getId());
            return Messenger.builder()
                    .code(200)
                    .message("자기소개글 수정 성공")
                    .data(updatedModel)
                    .build();
        } catch (Exception e) {
            log.error("자기소개글 수정 중 오류 발생: userId={}", aboutModel.getUserId(), e);
            return Messenger.builder()
                    .code(500)
                    .message("자기소개글 수정 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public Messenger delete(Long userId) {
        if (userId == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        try {
            if (!aboutRepository.existsByUserId(userId)) {
                return Messenger.builder()
                        .code(404)
                        .message("자기소개글을 찾을 수 없습니다.")
                        .build();
            }

            aboutRepository.deleteByUserId(userId);
            log.info("자기소개글 삭제 성공: userId={}", userId);
            return Messenger.builder()
                    .code(200)
                    .message("자기소개글 삭제 성공")
                    .build();
        } catch (Exception e) {
            log.error("자기소개글 삭제 중 오류 발생: userId={}", userId, e);
            return Messenger.builder()
                    .code(500)
                    .message("자기소개글 삭제 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
}

