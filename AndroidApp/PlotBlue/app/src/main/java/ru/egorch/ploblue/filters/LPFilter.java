package ru.egorch.ploblue.filters;

/**
 * Фильтр высоких частот
 */
public class LPFilter {
    private float Klf;

    private boolean status;

    public LPFilter(float klf) {
        Klf = klf;
        status = true;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * Установить коэффицент фильтрации от 0 до 1
     * @param klf
     */
    public void setKlf(float klf) {
        if(klf >= 0.0 && klf <= 1){
            Klf = klf;
        }
    }

    /**
     * Иттерация фильтра низких частот
     * @param X
     * @param Y
     * @return отфитрованный сигнал
     */
    public float LPF(float X, float Y){
        if(status){
            return (Klf*X) + (1-Klf)*Y;
        }

        return Y;
    }
}
