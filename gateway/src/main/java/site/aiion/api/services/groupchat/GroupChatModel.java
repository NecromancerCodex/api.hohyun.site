package site.aiion.api.services.groupchat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatModel {
    private Long id;
    private Long userId;
    private String username;
    private String message;
    private LocalDateTime createdAt;
}

