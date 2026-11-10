package br.com.raizesdonordeste.domain.exception;

/** Credenciais inválidas no login. Traduzida para HTTP 401. */
public class NaoAutenticadoException extends DominioException {
    public NaoAutenticadoException(String mensagem) {
        super("CREDENCIAIS_INVALIDAS", mensagem, null);
    }
}
