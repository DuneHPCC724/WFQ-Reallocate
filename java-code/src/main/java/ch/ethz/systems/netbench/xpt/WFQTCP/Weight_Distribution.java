package ch.ethz.systems.netbench.xpt.WFQTCP;

public class Weight_Distribution {
    private float[] weights;
    private int weight_num;
    public Weight_Distribution(String distribution, int weight_num){
        switch (distribution) {
            case "uniform":
                this.weight_num = weight_num;
                this.weights = new float[this.weight_num];
                double weight_each = 1.0/(weight_num*1.0);
                for(int i=0;i<this.weight_num;i++){
                    this.weights[i] = (float) weight_each;
                }
                return;
            default:
                return;
        }
    }

    public float[] get_weights(){
        return this.weights;
    }
}
