package unprotesting.com.github.data.ephemeral.data;

import lombok.Getter;
import lombok.Setter;

//  Enchantment data class for storing general item data

public class EnchantmentData{

    @Getter
    private int buys,
                sells;
    @Getter @Setter
    private double price,
                   ratio;

    public EnchantmentData(double price, double ratio){
        this.buys = 0;
        this.sells = 0;
        this.price = price;
        this.ratio = ratio;
    }

    public void increaseBuys(int amount){
        this.buys = this.buys+amount;
    }

    public void increaseSells(int amount){
        this.sells = this.sells + amount;
    }

}
