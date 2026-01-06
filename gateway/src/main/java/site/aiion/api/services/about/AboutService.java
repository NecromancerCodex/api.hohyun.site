package site.aiion.api.services.about;

import site.aiion.api.services.about.common.domain.Messenger;

public interface AboutService {
    /**
     * 사용자 ID로 자기소개글 조회
     */
    public Messenger findByUserId(Long userId);
    
    /**
     * 자기소개글 저장 (생성)
     */
    public Messenger save(AboutModel aboutModel);
    
    /**
     * 자기소개글 수정
     */
    public Messenger update(AboutModel aboutModel);
    
    /**
     * 자기소개글 삭제
     */
    public Messenger delete(Long userId);
}

