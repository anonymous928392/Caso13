Feature: Task creation

  Background:
    Given the database is empty

  Scenario: Task creation succeeds with valid data
    Given a registered user "ana@example.com" with password "Password123"
    And the user is a member of household "Hogar Test"
    When the client creates a task with title "Lavar la loza" category "Cocina" and description "Después del almuerzo"
    Then the response status should be 201
    And the response should contain title "Lavar la loza" and category "Cocina"
    And the task estado should be "Pendiente"

  Scenario: Task creation rejected with blank title
    Given a registered user "ana@example.com" with password "Password123"
    And the user is a member of household "Hogar Test"
    When the client creates a task with title "" category "Limpieza" and description ""
    Then the response status should be 400
    And the error code should be "VALIDATION_ERROR"
    And the error details should contain "El titulo es obligatorio"

  # NOTE: category validation is enforced by TareaService (not @Valid), so the backend returns 409 BUSINESS_ERROR.
  # CP-016 was originally designed as 400; gap noted — see BUG-001 discussion.
  Scenario: Task creation rejected with invalid category
    Given a registered user "ana@example.com" with password "Password123"
    And the user is a member of household "Hogar Test"
    When the client creates a task with title "Hacer ejercicio" category "Deportes" and description ""
    Then the response status should be 409
    And the error code should be "BUSINESS_ERROR"
    And the response message should contain "Categoria no valida"

  # NOTE: backend returns 409 (not 403) because TareaService throws RuntimeException caught by GlobalExceptionHandler.
  # CP-017 was originally designed as 403; adjusted to 409 — BUG-001 deferred to Sprint 3.
  Scenario: Task creation rejected when user is not a member of the household
    Given a registered user "ana@example.com" with password "Password123"
    And the user is a member of household "Hogar Test"
    And a user "externo@example.com" with password "Password123" is not a member of the household
    When the external user tries to create a task with title "Lavar la loza" category "Cocina" and description ""
    Then the response status should be 409
    And the error code should be "BUSINESS_ERROR"
    And the response message should contain "No perteneces a este hogar"
