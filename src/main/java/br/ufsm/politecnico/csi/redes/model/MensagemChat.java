package br.ufsm.politecnico.csi.redes.model;

import br.ufsm.politecnico.csi.redes.chat.ChatClientSwing;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.swing.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MensagemChat {
    private String mensagem;
    private String usuario;
}
