package site.aiion.api.services.groupchat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.aiion.api.services.about.common.domain.Messenger;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatServiceImpl implements GroupChatService {

    private final GroupChatRepository groupChatRepository;

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

    private GroupChat modelToEntity(GroupChatModel model) {
        return GroupChat.builder()
                .userId(model.getUserId())
                .username(model.getUsername())
                .message(model.getMessage())
                .build();
    }

    @Override
    @Transactional
    public Messenger save(GroupChatModel groupChatModel) {
        if (groupChatModel == null) {
            return Messenger.builder()
                    .code(400)
                    .message("메시지 정보가 필요합니다.")
                    .build();
        }

        if (groupChatModel.getUserId() == null) {
            return Messenger.builder()
                    .code(400)
                    .message("사용자 ID가 필요합니다.")
                    .build();
        }

        if (groupChatModel.getMessage() == null || groupChatModel.getMessage().trim().isEmpty()) {
            return Messenger.builder()
                    .code(400)
                    .message("메시지 내용이 필요합니다.")
                    .build();
        }

        try {
            GroupChat entity = modelToEntity(groupChatModel);
            GroupChat savedEntity = groupChatRepository.save(entity);
            GroupChatModel savedModel = entityToModel(savedEntity);

            log.info("그룹 채팅 메시지 저장 성공: userId={}, id={}", groupChatModel.getUserId(), savedEntity.getId());
            return Messenger.builder()
                    .code(200)
                    .message("메시지 전송 성공")
                    .data(savedModel)
                    .build();
        } catch (Exception e) {
            log.error("그룹 채팅 메시지 저장 중 오류 발생: userId={}", groupChatModel.getUserId(), e);
            return Messenger.builder()
                    .code(500)
                    .message("메시지 전송 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public Messenger findAll(Pageable pageable) {
        try {
            Page<GroupChat> page = groupChatRepository.findAllByOrderByCreatedAtDesc(pageable);
            List<GroupChatModel> messages = page.getContent().stream()
                    .map(this::entityToModel)
                    .collect(Collectors.toList());

            return Messenger.builder()
                    .code(200)
                    .message("메시지 목록 조회 성공")
                    .data(messages)
                    .build();
        } catch (Exception e) {
            log.error("그룹 채팅 메시지 목록 조회 중 오류 발생", e);
            return Messenger.builder()
                    .code(500)
                    .message("메시지 목록 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public Messenger findRecentMessages(int limit) {
        try {
            List<GroupChat> entities = groupChatRepository.findTop50ByOrderByCreatedAtDesc();
            // limit 파라미터로 실제 필요한 만큼만 반환
            List<GroupChatModel> messages = entities.stream()
                    .limit(limit)
                    .map(this::entityToModel)
                    .collect(Collectors.toList());

            return Messenger.builder()
                    .code(200)
                    .message("최근 메시지 조회 성공")
                    .data(messages)
                    .build();
        } catch (Exception e) {
            log.error("최근 메시지 조회 중 오류 발생", e);
            return Messenger.builder()
                    .code(500)
                    .message("최근 메시지 조회 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
}

