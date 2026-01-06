# OAuth 플로우 점검 결과

## 현재 구현 상태

### ✅ 잘 구현된 부분

1. **구글 사용자 정보 추출**
   - `GoogleOAuthService.getUserInfo()`: Access Token으로 사용자 정보 조회
   - `extractUserInfo()`: email, name, id 추출
   - ✅ email 추출 완료
   - ⚠️ "id" 사용 (OpenID Connect의 "sub" 권장)

2. **사용자 조회/생성**
   - `findByEmailAndProvider()`: email + provider로 조회
   - 없으면 자동 생성
   - ✅ 기본 로직 구현 완료

3. **토큰 생성 및 저장**
   - ✅ Access Token: Redis에 저장
   - ✅ Refresh Token: User 테이블에 저장
   - ✅ Refresh Token: HttpOnly 쿠키로 설정
   - ✅ Access Token: URL에 포함하여 리다이렉트

### ⚠️ 개선이 필요한 부분

1. **구글 사용자 ID 추출**
   - 현재: `userInfo.get("id")` 사용
   - 권장: OpenID Connect의 `sub` 필드 사용
   - 구글 v2/userinfo API는 "id"를 반환하지만, OpenID Connect 표준은 "sub"입니다.

2. **사용자 조회 전략**
   - 현재: `email + provider`로만 조회
   - 문제: email이 변경될 수 있음
   - 권장: `providerId + provider`로 우선 조회 (sub는 변하지 않음)

3. **Spring Security 미사용**
   - 현재: 수동으로 OAuth 플로우 구현
   - 장점: 세밀한 제어 가능
   - 단점: 보안 취약점 가능성, 코드 복잡도 증가

## ✅ 적용 완료된 개선 사항

### 1. ✅ providerId로 사용자 조회 추가

```java
// UserRepository에 추가됨
Optional<User> findByProviderIdAndProvider(String providerId, String provider);

// UserService에 추가됨
Messenger findByProviderIdAndProvider(String providerId, String provider);
```

### 2. ✅ 사용자 조회 우선순위 변경

```java
// GoogleController.googleCallback()에서 구현됨
// 1순위: providerId + provider로 조회 (가장 안정적, sub는 변하지 않음)
// 2순위: email + provider로 조회 (하위 호환성)
// 3순위: 없으면 새로 생성
```

### 3. ✅ OpenID Connect sub 필드 지원

```java
// GoogleOAuthService.extractUserInfo() 수정됨
String sub = (String) userInfo.get("sub");  // OpenID Connect 표준
String googleId = sub != null ? sub : (String) userInfo.get("id");  // 폴백
```

## 현재 플로우 요약 (개선 후)

```
1. 프론트엔드 → GET /api/google/auth-url
   ↓
2. 구글 OAuth 인증 페이지
   ↓
3. 구글 → GET /api/google/callback?code=...
   ↓
4. GoogleController.googleCallback():
   a. Authorization Code → Access Token 교환
   b. Access Token → 사용자 정보 조회
   c. sub(또는 id), email, name 추출
   d. 1순위: findByProviderIdAndProvider(sub, "google")로 조회
   e. 2순위: findByEmailAndProvider(email, "google")로 조회 (하위 호환성)
   f. 3순위: 없으면 새로 생성
   g. JWT Access/Refresh 토큰 생성
   h. Access Token → Redis 저장
   i. Refresh Token → User 테이블 저장
   j. Refresh Token → HttpOnly 쿠키 설정
   k. Access Token → URL에 포함하여 리다이렉트
   ↓
5. 프론트엔드 → /login/callback?token=...
```

## 개선 효과

1. **안정성 향상**: providerId(sub)는 변하지 않으므로 email 변경에도 안정적으로 사용자 식별 가능
2. **표준 준수**: OpenID Connect의 sub 필드 우선 사용
3. **하위 호환성**: 기존 email 기반 조회도 유지하여 기존 사용자 지원

## Spring Security 사용 여부

**현재: Spring Security 미사용** ✅ (수동 구현)

**장점:**
- 세밀한 제어 가능
- 커스텀 플로우 구현 용이
- 의존성 감소

**단점:**
- 보안 취약점 가능성
- 코드 복잡도 증가
- 표준 플로우와 차이

**결론:** 현재 구현이 잘 되어 있으므로 Spring Security 도입은 선택사항입니다.

