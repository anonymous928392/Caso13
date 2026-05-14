package com.eap08.domesticas.acceptance;

import io.cucumber.spring.ScenarioScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class ScenarioContext {

    private ResponseEntity<String> lastResponse;

    public ResponseEntity<String> getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(ResponseEntity<String> response) {
        this.lastResponse = response;
    }
}
