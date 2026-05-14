package com.eap08.domesticas.service;

import com.eap08.domesticas.dto.TareaRequest;
import com.eap08.domesticas.dto.TareaResponse;
import com.eap08.domesticas.model.Hogar;
import com.eap08.domesticas.model.Tarea;
import com.eap08.domesticas.model.Usuario;
import com.eap08.domesticas.repository.HogarRepository;
import com.eap08.domesticas.repository.TareaRepository;
import com.eap08.domesticas.repository.UsuarioHogarRepository;
import com.eap08.domesticas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock
    private TareaRepository tareaRepo;

    @Mock
    private HogarRepository hogarRepo;

    @Mock
    private UsuarioHogarRepository usuarioHogarRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private TareaService tareaService;

    @Test
    void shouldCreateTaskSuccessfully() {
        // Arrange
        Long hogarId = 1L;
        String emailCreador = "ana@example.com";

        TareaRequest.CreateTareaRequest request = new TareaRequest.CreateTareaRequest(
                "Lavar la loza", "Después del almuerzo", "Cocina", null, null);

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(10L);
        usuario.setEmail(emailCreador);

        Hogar hogar = Hogar.builder().hogarId(hogarId).nombre("Hogar Test").build();

        Tarea tareaGuardada = Tarea.builder()
                .tareaId(100L)
                .hogar(hogar)
                .titulo("Lavar la loza")
                .descripcion("Después del almuerzo")
                .categoria("Cocina")
                .estado(Tarea.ESTADO_PENDIENTE)
                .build();

        when(usuarioRepo.findByEmail(emailCreador)).thenReturn(Optional.of(usuario));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(10L, hogarId)).thenReturn(true);
        when(hogarRepo.getReferenceById(hogarId)).thenReturn(hogar);
        when(tareaRepo.save(any(Tarea.class))).thenReturn(tareaGuardada);

        // Act
        TareaResponse.TareaData result = tareaService.crearTarea(hogarId, request, emailCreador);

        // Assert
        assertThat(result.titulo()).isEqualTo("Lavar la loza");
        assertThat(result.categoria()).isEqualTo("Cocina");
        assertThat(result.estado()).isEqualTo(Tarea.ESTADO_PENDIENTE);
        assertThat(result.hogarId()).isEqualTo(hogarId);

        ArgumentCaptor<Tarea> tareaCaptor = ArgumentCaptor.forClass(Tarea.class);
        verify(tareaRepo).save(tareaCaptor.capture());
        Tarea saved = tareaCaptor.getValue();
        assertThat(saved.getTitulo()).isEqualTo("Lavar la loza");
        assertThat(saved.getCategoria()).isEqualTo("Cocina");
        assertThat(saved.getEstado()).isEqualTo(Tarea.ESTADO_PENDIENTE);
        assertThat(saved.getDescripcion()).isEqualTo("Después del almuerzo");
    }

    @Test
    void shouldThrowWhenCategoryIsInvalid() {
        // Arrange
        Long hogarId = 1L;
        String emailCreador = "ana@example.com";

        TareaRequest.CreateTareaRequest request = new TareaRequest.CreateTareaRequest(
                "Hacer ejercicio", "", "Deportes", null, null);

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(10L);
        usuario.setEmail(emailCreador);

        when(usuarioRepo.findByEmail(emailCreador)).thenReturn(Optional.of(usuario));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(10L, hogarId)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> tareaService.crearTarea(hogarId, request, emailCreador))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoria no valida");

        verify(tareaRepo, never()).save(any(Tarea.class));
    }

    @Test
    void shouldThrowWhenUserIsNotMemberOfHousehold() {
        // Arrange
        Long hogarId = 1L;
        String emailCreador = "externo@example.com";

        TareaRequest.CreateTareaRequest request = new TareaRequest.CreateTareaRequest(
                "Lavar la loza", "", "Cocina", null, null);

        Usuario usuario = new Usuario();
        usuario.setUsuarioId(20L);
        usuario.setEmail(emailCreador);

        when(usuarioRepo.findByEmail(emailCreador)).thenReturn(Optional.of(usuario));
        when(usuarioHogarRepo.existsByIdUsuarioIdAndIdHogarId(20L, hogarId)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> tareaService.crearTarea(hogarId, request, emailCreador))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No perteneces a este hogar");

        verify(tareaRepo, never()).save(any(Tarea.class));
    }
}
