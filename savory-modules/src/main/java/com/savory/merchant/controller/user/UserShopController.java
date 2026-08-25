package com.savory.merchant.controller.user;

import com.savory.common.result.Result;
import com.savory.merchant.client.AiSearchClient;
import com.savory.merchant.service.CategoryService;
import com.savory.merchant.service.DishService;
import com.savory.merchant.service.MerchantInfoService;
import com.savory.merchant.service.SetmealService;
import com.savory.pojo.entity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端用户浏览接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户端浏览相关接口")
public class UserShopController {

    @Autowired
    private MerchantInfoService merchantInfoService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private AiSearchClient aiSearchClient;

    /**
     * 附近店铺列表
     * 返回所有营业中的商户
     *
     * @return
     */
    @GetMapping("/merchant/list")
    @Operation(summary = "附近店铺列表")
    public Result<List<MerchantInfo>> merchantList() {
        log.info("查询附近店铺列表");
        //1、查询所有营业中的商户
        List<MerchantInfo> list = merchantInfoService.listOpen();
        return Result.success(list);
    }

    /**
     * 店铺详情
     *
     * @param id
     * @return
     */
    @GetMapping("/merchant/{id}")
    @Operation(summary = "店铺详情")
    public Result<MerchantInfo> merchantDetail(@PathVariable Long id) {
        log.info("查询店铺详情，merchantId: {}", id);
        MerchantInfo merchant = merchantInfoService.getById(id);
        return Result.success(merchant);
    }

    /**
     * 根据店铺查分类
     *
     * @param merchantId
     * @param type
     * @return
     */
    @GetMapping("/category/list")
    @Operation(summary = "根据店铺查分类")
    public Result<List<Category>> categoryList(Long merchantId, Integer type) {
        log.info("查询分类列表，merchantId: {}, type: {}", merchantId, type);
        List<Category> list = categoryService.list(merchantId, type);
        return Result.success(list);
    }

    /**
     * 根据分类查菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/dish/list")
    @Operation(summary = "根据分类查菜品")
    public Result<List<Dish>> dishList(Long categoryId) {
        log.info("查询菜品列表，categoryId: {}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 根据分类查套餐
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/setmeal/list")
    @Operation(summary = "根据分类查套餐")
    public Result<List<Setmeal>> setmealList(Long categoryId) {
        log.info("查询套餐列表，categoryId: {}", categoryId);
        List<Setmeal> list = setmealService.listByCategoryId(categoryId);
        return Result.success(list);
    }

    /**
     * AI语义搜索菜品
     * 将用户自然语言查询发给 ai-service 做向量检索
     *
     * @param keyword 搜索关键词（支持自然语言描述）
     * @return
     */
    @GetMapping("/dish/search")
    @Operation(summary = "AI语义搜索菜品")
    public Result<List<Map<String, Object>>> dishSearch(@RequestParam String keyword) {
        log.info("AI语义搜索菜品: {}", keyword);
        try {
            List<Map<String, Object>> result = aiSearchClient.searchDish(keyword, 10);
            return Result.success(result);
        } catch (Exception e) {
            log.error("调用AI语义搜索失败: {}", e.getMessage());
            return Result.error("AI语义搜索服务暂不可用");
        }
    }
}
