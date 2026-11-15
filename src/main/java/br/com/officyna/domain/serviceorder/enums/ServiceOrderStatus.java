package br.com.officyna.domain.serviceorder.enums;

import lombok.Getter;

@Getter
public enum ServiceOrderStatus {
    RECEBIDA(1, "Recebida"),
    EM_DIAGNOSTICO(2, "Em diagnóstico"),
    AGUARDANDO_APROVACAO(3, "Aguardando aprovação"),
    EM_EXECUCAO(4, "Em execução"),
    FINALIZADA(5, "Finalizada"),
    APROVADA(6, "Aprovada"),
    ENTREGUE(7,"Entregue"),
    RECUSADA(8, "Recusada"),
    PENDENT_PAGAMENTO(9, "Pendente pagamento");

    private final Integer idStatus;

    private final String statusName;

    ServiceOrderStatus (Integer idStatus, String statusName){
        this.idStatus = idStatus;
        this.statusName = statusName;
    }

    public static ServiceOrderStatus fromId(Integer id) {
        for (ServiceOrderStatus status : ServiceOrderStatus.values()) {
            if (status.idStatus.equals(id)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ServiceOrderStatus ID: " + id);
    }

    public int getPriority() {
        return switch (this) {
            case EM_EXECUCAO -> 1;
            case AGUARDANDO_APROVACAO -> 2;
            case PENDENT_PAGAMENTO -> 3;
            case APROVADA -> 4;
            case EM_DIAGNOSTICO -> 5;
            case RECEBIDA -> 6;
            default -> Integer.MAX_VALUE;
        };
    }
}
