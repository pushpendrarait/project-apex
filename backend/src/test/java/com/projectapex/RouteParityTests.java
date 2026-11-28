package com.projectapex;

import com.projectapex.dto.request.SendRegistrationOtpRequest;
import com.projectapex.dto.request.VerifyRegistrationOtpRequest;
import com.projectapex.model.OtpSession;
import com.projectapex.service.FirebaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouteParityTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FirebaseService firebaseService;

    @Test
    @DisplayName("OTP Registration route aliases (/auth/registration/* and /auth/register/*) both work identically")
    void testRegistrationOtpRouteAliases() {
        verifyOtpRegistrationFlow("/api/v1/auth/registration/send-otp", "/api/v1/auth/registration/verify-otp",
                "+91 98111 44556", "parity.one@apex.dev", "Parity Player One");
        verifyOtpRegistrationFlow("/api/v1/auth/register/send-otp", "/api/v1/auth/register/verify-otp",
                "+91 98111 44557", "parity.two@apex.dev", "Parity Player Two");
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout succeeds with HTTP 200 for stateless JWT clients (Web/Android/iOS)")
    void testStatelessLogoutEndpoint() {
        ResponseEntity<Map<String, Object>> anonymousLogout = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, anonymousLogout.getStatusCode());
        assertNotNull(anonymousLogout.getBody());
        assertEquals(Boolean.TRUE, anonymousLogout.getBody().get("success"));
        assertEquals("Logged out successfully", anonymousLogout.getBody().get("message"));

        String token = registerViaAliasAndGetToken("+91 98111 44558", "parity.logout@apex.dev", "Logout Player");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map<String, Object>> authenticatedLogout = restTemplate.exchange(
                "/api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, authenticatedLogout.getStatusCode());
        assertNotNull(authenticatedLogout.getBody());
        assertEquals(Boolean.TRUE, authenticatedLogout.getBody().get("success"));
        assertEquals("Logged out successfully", authenticatedLogout.getBody().get("message"));
    }

    @Test
    @DisplayName("GET /api/v1/bookings/my and /api/v1/bookings/my-passes both invoke player passes handler")
    void testPlayerPassesRouteAlias() {
        String token = registerViaAliasAndGetToken("+91 98111 44559", "parity.passes@apex.dev", "Passes Player");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> authEntity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> myResp = restTemplate.exchange(
                "/api/v1/bookings/my",
                HttpMethod.GET,
                authEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, myResp.getStatusCode());
        assertNotNull(myResp.getBody());
        assertEquals(Boolean.TRUE, myResp.getBody().get("success"));
        assertEquals("Player bookings retrieved", myResp.getBody().get("message"));
        assertInstanceOf(List.class, myResp.getBody().get("data"));

        ResponseEntity<Map<String, Object>> myPassesResp = restTemplate.exchange(
                "/api/v1/bookings/my-passes",
                HttpMethod.GET,
                authEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, myPassesResp.getStatusCode());
        assertNotNull(myPassesResp.getBody());
        assertEquals(Boolean.TRUE, myPassesResp.getBody().get("success"));
        assertEquals("Player bookings retrieved", myPassesResp.getBody().get("message"));
        assertEquals(myResp.getBody().get("data"), myPassesResp.getBody().get("data"));
    }

    private String verifyOtpRegistrationFlow(String sendPath, String verifyPath,
                                             String phone, String email, String displayName) {
        SendRegistrationOtpRequest sendReq = new SendRegistrationOtpRequest();
        sendReq.setPhone(phone);
        sendReq.setEmail(email);
        sendReq.setDisplayName(displayName);

        ResponseEntity<Map<String, Object>> sendResp = restTemplate.exchange(
                sendPath,
                HttpMethod.POST,
                new HttpEntity<>(sendReq),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, sendResp.getStatusCode());
        assertNotNull(sendResp.getBody());
        assertEquals(Boolean.TRUE, sendResp.getBody().get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> sendData = (Map<String, Object>) sendResp.getBody().get("data");
        assertNotNull(sendData);
        String sessionId = (String) sendData.get("sessionId");
        assertNotNull(sessionId);

        OtpSession session = firebaseService.getOtpVerifications().get(sessionId);
        assertNotNull(session);

        VerifyRegistrationOtpRequest verifyReq = new VerifyRegistrationOtpRequest();
        verifyReq.setSessionId(sessionId);
        verifyReq.setPhoneOtp(session.getPhoneOtp());
        verifyReq.setEmailOtp(session.getEmailOtp());
        verifyReq.setRole("player");
        verifyReq.setDisplayName(displayName);
        verifyReq.setPhone(phone);
        verifyReq.setEmail(email);
        verifyReq.setCity("Thane");

        ResponseEntity<Map<String, Object>> verifyResp = restTemplate.exchange(
                verifyPath,
                HttpMethod.POST,
                new HttpEntity<>(verifyReq),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, verifyResp.getStatusCode());
        assertNotNull(verifyResp.getBody());
        assertEquals(Boolean.TRUE, verifyResp.getBody().get("success"));

        @SuppressWarnings("unchecked")
        Map<String, Object> authData = (Map<String, Object>) verifyResp.getBody().get("data");
        assertNotNull(authData);
        String token = (String) authData.get("token");
        assertNotNull(token);
        assertFalse(token.isBlank());
        return token;
    }

    private String registerViaAliasAndGetToken(String phone, String email, String displayName) {
        return verifyOtpRegistrationFlow("/api/v1/auth/register/send-otp", "/api/v1/auth/register/verify-otp",
                phone, email, displayName);
    }
}
