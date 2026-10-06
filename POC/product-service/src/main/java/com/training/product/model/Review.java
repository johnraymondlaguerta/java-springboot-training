package com.training.product.model;

import java.util.Date;

/** EMBEDDED inside Product: a review belongs to one product and is read with it. */
public record Review(String author, int rating, String comment, Date created) {
}
