package com.chaike.shop.admin.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class CatalogController {
    private final JdbcTemplate jdbc;

    public CatalogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return jdbc.query("SELECT id, name, sort_order, active, created_at FROM product_category ORDER BY sort_order, id",
                (rs, row) -> new CategoryView(rs.getLong("id"), rs.getString("name"), rs.getInt("sort_order"),
                        rs.getBoolean("active"), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@Valid @RequestBody CategoryRequest request) {
        KeyHolder key = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO product_category (name, sort_order) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, request.name().trim());
                ps.setInt(2, request.sortOrder());
                return ps;
            }, key);
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }
        long id = key.getKey().longValue();
        return jdbc.queryForObject("SELECT id, name, sort_order, active, created_at FROM product_category WHERE id = ?",
                (rs, row) -> new CategoryView(rs.getLong("id"), rs.getString("name"), rs.getInt("sort_order"),
                        rs.getBoolean("active"), rs.getTimestamp("created_at").toLocalDateTime()), id);
    }

    @GetMapping("/products")
    public List<ProductView> products() {
        return jdbc.query("SELECT p.id, p.category_id, c.name AS category_name, p.name, p.description, p.price, p.stock, p.active, p.created_at " +
                        "FROM product p JOIN product_category c ON c.id = p.category_id ORDER BY p.id DESC",
                (rs, row) -> new ProductView(rs.getLong("id"), rs.getLong("category_id"), rs.getString("category_name"),
                        rs.getString("name"), rs.getString("description"), rs.getBigDecimal("price"), rs.getInt("stock"),
                        rs.getBoolean("active"), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView createProduct(@Valid @RequestBody ProductRequest request) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM product_category WHERE id = ?", Integer.class, request.categoryId()) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category does not exist");
        }
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO product (category_id, name, description, price, stock) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.categoryId());
            ps.setString(2, request.name().trim());
            ps.setString(3, request.description());
            ps.setBigDecimal(4, request.price());
            ps.setInt(5, request.stock());
            return ps;
        }, key);
        long id = key.getKey().longValue();
        return jdbc.queryForObject("SELECT p.id, p.category_id, c.name AS category_name, p.name, p.description, p.price, p.stock, p.active, p.created_at " +
                        "FROM product p JOIN product_category c ON c.id = p.category_id WHERE p.id = ?",
                (rs, row) -> new ProductView(rs.getLong("id"), rs.getLong("category_id"), rs.getString("category_name"),
                        rs.getString("name"), rs.getString("description"), rs.getBigDecimal("price"), rs.getInt("stock"),
                        rs.getBoolean("active"), rs.getTimestamp("created_at").toLocalDateTime()), id);
    }

    public record CategoryRequest(@NotBlank @Size(max = 80) String name, @Min(0) int sortOrder) { }
    public record ProductRequest(@NotNull @Min(1) Long categoryId, @NotBlank @Size(max = 160) String name,
                                 @Size(max = 4000) String description, @NotNull @DecimalMin("0.00") BigDecimal price,
                                 @Min(0) int stock) { }
    public record CategoryView(long id, String name, int sortOrder, boolean active, LocalDateTime createdAt) { }
    public record ProductView(long id, long categoryId, String categoryName, String name, String description,
                              BigDecimal price, int stock, boolean active, LocalDateTime createdAt) { }
}
