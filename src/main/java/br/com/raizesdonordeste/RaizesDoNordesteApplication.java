package br.com.raizesdonordeste;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada da API Raízes do Nordeste.
 *
 * Organização em camadas (requisito 4.3 do roteiro):
 *  - domain ......... entidades, enums e regras de negócio puras (ex.: transição de status, baixa de estoque)
 *  - application .... serviços/casos de uso que orquestram o fluxo (criar pedido, pagar, fidelidade...)
 *  - infrastructure . repositórios JPA, segurança JWT, gateway de pagamento mock, seed
 *  - api ............ controllers REST, contratos HTTP e tratamento padronizado de erros
 *
 * UserDetailsServiceAutoConfiguration é excluído porque a autenticação é feita por JWT
 * (JwtAuthenticationFilter); sem isso o Spring criaria um usuário/senha padrão sem uso.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class RaizesDoNordesteApplication {
    public static void main(String[] args) {
        SpringApplication.run(RaizesDoNordesteApplication.class, args);
    }
}
