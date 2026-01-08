package site.aiion.api.services.groupchat;

import site.aiion.api.services.about.common.domain.Messenger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GroupChatService {
    /**
     * 그룹 채팅 메시지 저장 (모든 사용자 가능)
     */
    Messenger save(GroupChatModel groupChatModel);
    
    /**
     * 그룹 채팅 메시지 목록 조회 (최신순, 페이징)
     */
    Messenger findAll(Pageable pageable);
    
    /**
     * 최근 메시지 N개 조회 (실시간 채팅용)
     */
    Messenger findRecentMessages(int limit);
}

