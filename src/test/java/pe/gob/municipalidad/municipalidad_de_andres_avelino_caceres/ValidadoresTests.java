package pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.enums.TipoDocumento;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.exception.BusinessException;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.DocumentoIdentidadValidator;
import pe.gob.municipalidad.municipalidad_de_andres_avelino_caceres.validation.RucValidator;

/**
 * Pruebas unitarias de los validadores de documento de identidad y RUC peruano.
 */
class ValidadoresTests {

    private final RucValidator rucValidator = new RucValidator();
    private final DocumentoIdentidadValidator docValidator = new DocumentoIdentidadValidator(rucValidator);

    @Test
    void dniValidoDeOchoDigitosSeAceptaYNormaliza() {
        assertThat(docValidator.validarYNormalizar(TipoDocumento.DNI, " 12345678 "))
                .isEqualTo("12345678");
    }

    @Test
    void dniInvalidoSeRechaza() {
        assertThatThrownBy(() -> docValidator.validarYNormalizar(TipoDocumento.DNI, "1234567"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("8 dígitos");
        assertThatThrownBy(() -> docValidator.validarYNormalizar(TipoDocumento.DNI, "1234567A"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> docValidator.validarYNormalizar(TipoDocumento.DNI, "   "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void carneExtranjeriaYPasaporteAplicanReglasEspecificas() {
        assertThat(docValidator.validarYNormalizar(TipoDocumento.CARNE_EXTRANJERIA, "ce123456789"))
                .isEqualTo("CE123456789");
        assertThatThrownBy(() -> docValidator.validarYNormalizar(TipoDocumento.CARNE_EXTRANJERIA, "12345"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("9 y 12");

        assertThat(docValidator.validarYNormalizar(TipoDocumento.PASAPORTE, "ab123456"))
                .isEqualTo("AB123456");
        assertThatThrownBy(() -> docValidator.validarYNormalizar(TipoDocumento.PASAPORTE, "123"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rucPeruanoConDigitoVerificadorCorrectoSeAcepta() {
        // 20100070970 y 10123456781 cumplen módulo 11
        assertThat(rucValidator.validarYNormalizar(" 20100070970 ")).isEqualTo("20100070970");
        assertThat(rucValidator.validarYNormalizar("10123456781")).isEqualTo("10123456781");
        assertThat(rucValidator.validarYNormalizarOpcional("")).isNull();
        assertThat(rucValidator.validarYNormalizarOpcional(null)).isNull();
    }

    @Test
    void rucInvalidoPorLongitudPrefijoODigitoVerificadorSeRechaza() {
        // menos de 11 dígitos
        assertThatThrownBy(() -> rucValidator.validarYNormalizar("2010007097"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("11 dígitos");
        // prefijo inválido (30)
        assertThatThrownBy(() -> rucValidator.validarYNormalizar("30100070970"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prefijo");
        // dígito verificador incorrecto (termina en 1 en vez de 0)
        assertThatThrownBy(() -> rucValidator.validarYNormalizar("20100070971"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dígito verificador");
    }
}
