package site.aiion.api.gateway.proxy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiServiceProxyController {

	private final RestTemplate restTemplate;

	// 환경 변수로 관리
	// Docker Compose: http://rag-service:8001, http://vision-service:8002
	// EC2 독립 인스턴스: http://chat.hohyun.site:8001, http://vision.hohyun.site:8002
	// Vision 서비스는 선택적 (나중에 배포 가능)
	@Value("${ai.service.rag.url:http://chat.hohyun.site:8001}")
	private String ragServiceUrl;

	@Value("${ai.service.vision.url:}")
	private String visionServiceUrl;

	public AiServiceProxyController(RestTemplate restTemplate)
	{
		this.restTemplate = restTemplate;
	}

	// YOLO 서비스 프록시 (vision-service 통합)
	@RequestMapping({"/yolo/**"})
	public ResponseEntity<String> proxyYoloService(
			@RequestBody(required = false) String body,
			HttpMethod method,
			HttpServletRequest request,
			@RequestHeader HttpHeaders headers)
	{
		// Vision 서비스가 배포되지 않은 경우 처리
		if (visionServiceUrl == null || visionServiceUrl.isEmpty()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"error\": \"Vision service is not available yet\"}");
		}
		return proxyRequest(visionServiceUrl + "/yolo", body, method, request, headers);
	}

	// RAG OpenAI 서비스 프록시
	@RequestMapping({"/rag/openai/**"})
	public ResponseEntity<String> proxyRagOpenAIService(
			@RequestBody(required = false) String body,
			HttpMethod method,
			HttpServletRequest request,
			@RequestHeader HttpHeaders headers)
	{
		// ragServiceUrl은 base URL만 포함 (예: http://chat.hohyun.site:8001)
		// /api/rag/openai/** → /rag/openai/**로 변환되어 ragServiceUrl과 결합
		return proxyRequest(ragServiceUrl, body, method, request, headers);
	}

	// RAG Llama 서비스 프록시
	@RequestMapping({"/rag/llama/**"})
	public ResponseEntity<String> proxyRagLlamaService(
			@RequestBody(required = false) String body,
			HttpMethod method,
			HttpServletRequest request,
			@RequestHeader HttpHeaders headers)
	{
		// ragServiceUrl은 base URL만 포함 (예: http://chat.hohyun.site:8001)
		// /api/rag/llama/** → /rag/llama/**로 변환되어 ragServiceUrl과 결합
		return proxyRequest(ragServiceUrl, body, method, request, headers);
	}

	// Diffusers 서비스 프록시 (vision-service 통합)
	@RequestMapping({"/diffusers/**"})
	public ResponseEntity<String> proxyDiffusersService(
			@RequestBody(required = false) String body,
			HttpMethod method,
			HttpServletRequest request,
			@RequestHeader HttpHeaders headers)
	{
		// Vision 서비스가 배포되지 않은 경우 처리
		if (visionServiceUrl == null || visionServiceUrl.isEmpty()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body("{\"error\": \"Vision service is not available yet\"}");
		}
		return proxyRequest(visionServiceUrl + "/diffusers", body, method, request, headers);
	}

	private ResponseEntity<String> proxyRequest(
			String baseUrl,
			String body,
			HttpMethod method,
			HttpServletRequest request,
			HttpHeaders headers)
	{

		String requestUri = request.getRequestURI();
		String queryString = request.getQueryString();

		// Remove the /api prefix from the request URI for the target service
		String path = requestUri.replaceFirst("/api", "");

		URI uri = UriComponentsBuilder.fromUriString(baseUrl)
				.path(path)
				.query(queryString)
				.build(true)
				.toUri();

		HttpHeaders proxyHeaders = new HttpHeaders();
		// Copy all original headers except Host and Content-Length
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements())
		{
			String headerName = headerNames.nextElement();
			if (!headerName.equalsIgnoreCase(HttpHeaders.HOST) && !headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
			{
				proxyHeaders.add(headerName, request.getHeader(headerName));
			}
		}

		// Ensure Content-Type is set if a body exists
		if (body != null && !body.isEmpty() && proxyHeaders.getContentType() == null)
		{
			proxyHeaders.setContentType(MediaType.APPLICATION_JSON); // Default to JSON if not specified
		}

		org.springframework.http.HttpEntity<String> httpEntity = new org.springframework.http.HttpEntity<>(body, proxyHeaders);

		try
		{
			ResponseEntity<String> responseEntity = restTemplate.exchange(uri, method, httpEntity, String.class);
			return ResponseEntity.status(responseEntity.getStatusCode())
					.headers(responseEntity.getHeaders())
					.body(responseEntity.getBody());
		}
		catch (HttpClientErrorException | HttpServerErrorException e)
		{
			return ResponseEntity.status(e.getStatusCode())
					.headers(e.getResponseHeaders())
					.body(e.getResponseBodyAsString());
		}
		catch (Exception e)
		{
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Proxy error: " + e.getMessage());
		}
	}
}

