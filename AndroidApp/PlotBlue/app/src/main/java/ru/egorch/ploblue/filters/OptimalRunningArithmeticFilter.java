package ru.egorch.ploblue.filters;

/**
 * Сглаживающий фильтр
 */
public class OptimalRunningArithmeticFilter {
    private float[] vals;
    private int t = 0;
    private float average = 0;
    private int num_read;

    private boolean status;

    public OptimalRunningArithmeticFilter(int num_read) {
        vals = new float[100];
        this.num_read = num_read;
        status = true;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public void setNum_read(int num_read) {
        if(num_read > -1 && num_read < 100){
            this.num_read = num_read;
        }
    }

    /**
     * Иттерация фильтра высоких частот
     * @param newVal
     * @return
     */
    public float iterationFilter(float newVal) {
        if(status){
            if (++t >= num_read){
                t = 0; // перемотка t
            }
            average -= vals[t];         // вычитаем старое
            average += newVal;          // прибавляем новое
            vals[t] = newVal;           // запоминаем в массив
            return (average / num_read);
        }

        return newVal;
    }
}
