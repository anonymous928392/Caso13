package com.eap08.domesticas.acceptance.steps;

import com.eap08.domesticas.acceptance.ScenarioContext;
import com.eap08.domesticas.model.Hogar;
import com.eap08.domesticas.model.Usuario;
import com.eap08.domesticas.model.UsuarioHogar;
import com.eap08.domesticas.model.UsuarioHogarId;
import com.eap08.domesticas.repository.HogarRepository;
import com.eap08.domesticas.repository.PasswordResetTokenRepository;
import com.eap08.domesticas.repository.TareaRepository;
import com.eap08.domesticas.repository.UsuarioHogarRepository;
import com.eap08.domesticas.repository.UsuarioRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TareaSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HogarRepository hogarRepository;

    @Autowired
    private UsuarioHogarRepository usuarioHogarRepository;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ScenarioContext context;

    private String currentEmail;
    private String currentRawPassword;
    private Long currentHogarId;
    private String currentJwt;
    private String externalJwt;

    // Runs before AuthSteps @Before (default order ~10000) to delete in cascade order
    @Before(order = 100)
    public void clearDatabase() {
        tareaRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        usuarioHogarRepository.deleteAll();
        hogarRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Given("a registered user {string} with password {string}")
    public void aRegisteredUser(String email, String password) {
        currentEmail = email;
        currentRawPassword = password;
        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuarioRepository.saveAndFlush(usuario);
    }

    @Given("the user is a member of household {string}")
    public void theUserIsAMemberOfHousehold(String nombreHogar) throws Exception {
        Hogar hogar = Hogar.builder().nombre(nombreHogar).build();
        hogar = hogarRepository.saveAndFlush(hogar);
        currentHogarId = hogar.getHogarId();

        Usuario usuario = usuarioRepository.findByEmail(currentEmail).orElseThrow();
        UsuarioHogar membership = UsuarioHogar.builder()
                .id(new UsuarioHogarId(usuario.getUsuarioId(), hogar.getHogarId()))
                .usuario(usuario)
                .hogar(hogar)
                .rol(UsuarioHogar.ROL_ADMINISTRADOR)
                .build();
        usuarioHogarRepository.saveAndFlush(membership);

        Map<String, Object> loginBody = Map.of("email", currentEmail, "password", currentRawPassword);
        ResponseEntity<String> loginResponse = post("/api/auth/login", loginBody);
        Map<String, Object> loginData = objectMapper.readValue(loginResponse.getBody(), new TypeReference<>() {});
        currentJwt = (String) loginData.get("token");
    }

    @Given("a user {string} with password {string} is not a member of the household")
    public void aUserIsNotMemberOfHousehold(String email, String password) throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Externo");
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuarioRepository.saveAndFlush(usuario);

        Map<String, Object> loginBody = Map.of("email", email, "password", password);
        ResponseEntity<String> loginResponse = post("/api/auth/login", loginBody);
        Map<String, Object> loginData = objectMapper.readValue(loginResponse.getBody(), new TypeReference<>() {});
        externalJwt = (String) loginData.get("token");
    }

    @When("the client creates a task with title {string} category {string} and description {string}")
    public void theClientCreatesATask(String title, String categoria, String descripcion) throws Exception {
        Map<String, Object> body = Map.of(
                "titulo", title,
                "categoria", categoria,
                "descripcion", descripcion
        );
        context.setLastResponse(postAuth("/api/households/" + currentHogarId + "/tasks", body, currentJwt));
    }

    @When("the external user tries to create a task with title {string} category {string} and description {string}")
    public void theExternalUserTriesToCreateATask(String title, String categoria, String descripcion) throws Exception {
        Map<String, Object> body = Map.of(
                "titulo", title,
                "categoria", categoria,
                "descripcion", descripcion
        );
        context.setLastResponse(postAuth("/api/households/" + currentHogarId + "/tasks", body, externalJwt));
    }

    @Then("the response should contain title {string} and category {string}")
    public void theResponseShouldContainTitleAndCategory(String title, String categoria) throws Exception {
        Map<String, Object> body = responseAsMap();
        assertThat(body.get("titulo")).isEqualTo(title);
        assertThat(body.get("categoria")).isEqualTo(categoria);
    }

    @Then("the task estado should be {string}")
    public void theTaskEstadoShouldBe(String estado) throws Exception {
        Map<String, Object> body = responseAsMap();
        assertThat(body.get("estado")).isEqualTo(estado);
    }

    @Then("the error details should contain {string}")
    public void theErrorDetailsShouldContain(String expectedMessage) throws Exception {
        Map<String, Object> body = responseAsMap();
        List<String> details = (List<String>) body.get("details");
        assertThat(details).contains(expectedMessage);
    }

    private ResponseEntity<String> post(String path, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        return restTemplate.postForEntity(url(path), entity, String.class);
    }

    private ResponseEntity<String> postAuth(String path, Object body, String jwt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwt);
        String json = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        return restTemplate.postForEntity(url(path), entity, String.class);
    }

    private Map<String, Object> responseAsMap() throws Exception {
        if (context.getLastResponse().getBody() == null || context.getLastResponse().getBody().isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(context.getLastResponse().getBody(), new TypeReference<Map<String, Object>>() {});
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
