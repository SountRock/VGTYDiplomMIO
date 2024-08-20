package ru.egorch.ploblue.filters;

/**
 * Фильтр высоких частот
 */
public class HPFilter {
    private float Khf;
    private float K0 = 0.0F;
    private float K1 = 0.0F;

    private boolean status;

    public HPFilter(float khf) {
        Khf = khf;
        status = true;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * Установить коэффицент фильтрации от 0 до 1
     * @param Khf
     */
    public void setKhf(float Khf) {
        if(Khf >= 0.0 && Khf <= 1){
            this.Khf = Khf;
        }
    }

    /**
     * Иттерация фильтра выскоих частот
     * @param X
     * @return
     */
    public float DCRemover(float X){
        if(status){
            float Y_out = 0;
            K0=X+Khf*K1;
            Y_out=K0-K1;
            K1=K0;
            return Y_out;
        }

        return X;
    }
}
