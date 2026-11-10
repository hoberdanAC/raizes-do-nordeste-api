package br.com.raizesdonordeste.api.error;

import br.com.raizesdonordeste.api.error.ErroPadrao.DetalheErro;
import br.com.raizesdonordeste.application.port.GatewayIndisponivelException;
import br.com.raizesdonordeste.application.port.PagamentoRecusadoException;
import br.com.raizesdonordeste.domain.exception.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * Converte TODAS as exceções em respostas HTTP no formato ErroPadrao.
 * O tratamento é centralizado: nenhuma rota monta erro manualmente.
 *
 * Mapa de status:
 *  400 JSON malformado / valor de ENUM inválido / parâmetro inválido
 *  401 credenciais inválidas            402 pagamento recusado pelo gateway
 *  403 sem permissão / dado de terceiro 404 recurso inexistente
 *  409 regra de negócio / conflito      422 validação de campos
 *  503 gateway de pagamento fora do ar  500 erro inesperado (sem vazar stack trace)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Exceções do domínio ----------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroPadrao> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, e.getCodigo(), e.getMessage(), detalhes(e), req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroPadrao> regraNegocio(RegraNegocioException e, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage(), detalhes(e), req);
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroPadrao> dadosInvalidos(DadosInvalidosException e, HttpServletRequest req) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, e.getCodigo(), e.getMessage(), detalhes(e), req);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroPadrao> acessoNegadoDominio(AcessoNegadoException e, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, e.getCodigo(), e.getMessage(), null, req);
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ErroPadrao> naoAutenticado(NaoAutenticadoException e, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, e.getCodigo(), e.getMessage(), null, req);
    }

    // ---------- Pagamento (integração externa) ----------

    @ExceptionHandler(PagamentoRecusadoException.class)
    public ResponseEntity<ErroPadrao> pagamentoRecusado(PagamentoRecusadoException e, HttpServletRequest req) {
        return responder(HttpStatus.PAYMENT_REQUIRED, "PAGAMENTO_RECUSADO", e.getMessage(), List.of(
                new DetalheErro("pagamentoId", String.valueOf(e.getPagamentoId())),
                new DetalheErro("statusPedido", "AGUARDANDO_PAGAMENTO - é possível tentar outro método")), req);
    }

    @ExceptionHandler(GatewayIndisponivelException.class)
    public ResponseEntity<ErroPadrao> gatewayIndisponivel(GatewayIndisponivelException e, HttpServletRequest req) {
        List<DetalheErro> det = e.getPagamentoId() == null ? null
                : List.of(new DetalheErro("pagamentoId", String.valueOf(e.getPagamentoId())));
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "GATEWAY_INDISPONIVEL", e.getMessage(), det, req);
    }

    // ---------- Validação de entrada ----------

    /** @Valid falhou (campo obrigatório ausente, quantidade negativa, e-mail inválido...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroPadrao> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<DetalheErro> det = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new DetalheErro(f.getField(), f.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO_FALHOU", "Um ou mais campos são inválidos.", det, req);
    }

    /** JSON malformado ou valor fora do ENUM (ex.: "canalPedido": "DRONE"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroPadrao> jsonInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        if (e.getCause() instanceof InvalidFormatException ife) {
            String campo = caminho(ife);
            String problema = "valor '" + ife.getValue() + "' inválido";
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                problema += ". Aceitos: " + Arrays.toString(ife.getTargetType().getEnumConstants());
            }
            return responder(HttpStatus.BAD_REQUEST, "VALOR_INVALIDO", "Valor inválido no corpo da requisição.",
                    List.of(new DetalheErro(campo, problema)), req);
        }
        if (e.getCause() instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            return responder(HttpStatus.BAD_REQUEST, "VALOR_INVALIDO", "Tipo de dado inválido no corpo da requisição.",
                    List.of(new DetalheErro(caminho(jme), "tipo incompatível")), req);
        }
        return responder(HttpStatus.BAD_REQUEST, "JSON_INVALIDO", "Corpo da requisição ausente ou malformado.", null, req);
    }

    /** Query/path param com tipo errado (ex.: ?canalPedido=XPTO ou /pedidos/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroPadrao> parametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        String problema = "valor '" + e.getValue() + "' inválido";
        Class<?> tipo = e.getRequiredType();
        if (tipo != null && tipo.isEnum()) {
            problema += ". Aceitos: " + Arrays.toString(tipo.getEnumConstants());
        }
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Parâmetro inválido.",
                List.of(new DetalheErro(e.getName(), problema)), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroPadrao> parametroAusente(MissingServletRequestParameterException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "PARAMETRO_OBRIGATORIO", "Parâmetro obrigatório ausente.",
                List.of(new DetalheErro(e.getParameterName(), "obrigatório")), req);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErroPadrao> headerAusente(MissingRequestHeaderException e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, "HEADER_OBRIGATORIO", "Header obrigatório ausente.",
                List.of(new DetalheErro(e.getHeaderName(), "obrigatório")), req);
    }

    // ---------- Infra / genéricos ----------

    /** @PreAuthorize negou (perfil sem permissão). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroPadrao> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Seu perfil não tem permissão para esta operação.", null, req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroPadrao> rotaInexistente(NoResourceFoundException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, "ROTA_NAO_ENCONTRADA", "Rota não encontrada.", null, req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroPadrao> metodoNaoSuportado(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_SUPORTADO", "Método HTTP não suportado nesta rota.", null, req);
    }

    /** Corpo enviado sem "Content-Type: application/json". */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroPadrao> tipoMidia(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TIPO_DE_CONTEUDO_INVALIDO",
                "Envie o corpo como application/json.", null, req);
    }

    /** Violação de constraint do banco (ex.: duas requisições simultâneas com o mesmo e-mail). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroPadrao> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS", "A operação conflita com dados existentes.", null, req);
    }

    /** Qualquer erro não previsto: loga internamente, mas não expõe detalhes técnicos ao cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroPadrao> inesperado(Exception e, HttpServletRequest req) {
        ErroPadrao erro = ErroPadrao.de("ERRO_INTERNO", "Erro inesperado. Informe o requestId ao suporte.", null, req.getRequestURI());
        log.error("Erro inesperado requestId={} path={}", erro.requestId(), req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

    // ---------- Auxiliares ----------

    private ResponseEntity<ErroPadrao> responder(HttpStatus status, String codigo, String mensagem,
                                                 List<DetalheErro> detalhes, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErroPadrao.de(codigo, mensagem, detalhes, req.getRequestURI()));
    }

    private List<DetalheErro> detalhes(DominioException e) {
        return e.getViolacoes().stream().map(v -> new DetalheErro(v.campo(), v.problema())).toList();
    }

    /** Monta o caminho do campo com problema, ex.: itens[0].quantidade */
    private String caminho(JsonMappingException e) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : e.getPath()) {
            if (ref.getFieldName() != null) {
                if (!sb.isEmpty()) sb.append('.');
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.isEmpty() ? "body" : sb.toString();
    }
}
