package site.aiion.api.services.groupchat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    // 최신 메시지부터 조회 (페이징)
    Page<GroupChat> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 최신 메시지 N개 조회
    List<GroupChat> findTop50ByOrderByCreatedAtDesc();
}

