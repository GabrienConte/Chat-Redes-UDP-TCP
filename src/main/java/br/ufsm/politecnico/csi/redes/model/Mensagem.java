package br.ufsm.politecnico.csi.redes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Mensagem {

    private String tipoMensagem;
    private String usuario;
    private String status;

}
