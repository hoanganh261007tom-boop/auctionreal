package model;

public class Item {

    private int id;
    private String name;
    private String description;
    private double price;
    private double minStep;
    private int duration;
    private String seller;
    private String category;
    private String condition;
    private String status;

    public Item(
            int id,
            String name,
            String description,
            double price,
            double minStep,
            int duration,
            String seller,
            String category,
            String condition,
            String status
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.minStep = minStep;
        this.duration = duration;
        this.seller = seller;
        this.category = category;
        this.condition = condition;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public double getMinStep() {
        return minStep;
    }

    public int getDuration() {
        return duration;
    }

    public String getSeller() {
        return seller;
    }

    public String getCategory() {
        return category;
    }

    public String getCondition() {
        return condition;
    }

    public String getStatus() {
        return status;
    }
}