package br.com.fiap.soat15.osservice.config.security;

import br.com.fiap.soat15.osservice.adapter.out.persistence.repository.UsuarioRepository;
import br.com.fiap.soat15.osservice.domain.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("Deve retornar UserDetails quando usuário existe")
    void deveRetornarUserDetailsQuandoUsuarioExiste() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("admin@oficina.com")
                .password("senha-hash")
                .build();

        when(usuarioRepository.findByUsername("admin@oficina.com")).thenReturn(Optional.of(usuario));

        UserDetails resultado = usuarioDetailsService.loadUserByUsername("admin@oficina.com");

        assertThat(resultado.getUsername()).isEqualTo("admin@oficina.com");
        assertThat(resultado.getPassword()).isEqualTo("senha-hash");
        assertThat(resultado.getAuthorities()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não existe")
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByUsername("inexistente@oficina.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioDetailsService.loadUserByUsername("inexistente@oficina.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inexistente@oficina.com");
    }

    @Test
    @DisplayName("Deve atribuir role ADMIN ao usuário carregado")
    void deveAtribuirRoleAdmin() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .username("admin@oficina.com")
                .password("senha-hash")
                .build();

        when(usuarioRepository.findByUsername("admin@oficina.com")).thenReturn(Optional.of(usuario));

        UserDetails resultado = usuarioDetailsService.loadUserByUsername("admin@oficina.com");

        boolean temRoleAdmin = resultado.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        assertThat(temRoleAdmin).isTrue();
    }
}
