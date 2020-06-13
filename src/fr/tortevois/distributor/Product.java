package fr.tortevois.distributor;

import fr.tortevois.exception.ProductNotAvailable;

import static fr.tortevois.gateway.IGateway.CURRENCY;

public class Product {
    public final static int PRODUCT_ID = 0;
    public final static int PRODUCT_NAME = 1;
    public final static int PRODUCT_PRICE = 2;
    public final static int PRODUCT_QUANTITY = 3;

    private int id, quantity;
    private float price;
    private String name;

    /**
     * The product constructor's
     *
     * @param id       : the product ID
     * @param name     : the product name
     * @param price    : the product price
     * @param quantity : the available product quantity
     */
    public Product(int id, String name, float price, int quantity) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * Get the product ID
     *
     * @return The product ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the product name
     *
     * @return The product name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the product price
     *
     * @return The product price
     */
    public float getPrice() {
        return price;
    }

    /**
     * Get the available product quantity
     *
     * @return The available product quantity
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Retrieve one product
     *
     * @throws ProductNotAvailable
     */
    public void retrieveOne() throws ProductNotAvailable {
        if (quantity > 0) {
            quantity--;
        } else {
            throw new ProductNotAvailable();
        }
    }

    /**
     * Get the Item menu
     */
    public void getItemMenu() {
        if (quantity > 0) {
            System.out.println(String.format("%3d | %-50s |   %5.2f%s  | %7d", id, name, price, CURRENCY, quantity));
        }
    }
}
