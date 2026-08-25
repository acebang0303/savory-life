package com.savory.merchant.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import com.savory.common.result.PageResult;
import com.savory.merchant.mapper.MerchantInfoMapper;
import com.savory.merchant.service.MerchantInfoService;
import com.savory.pojo.entity.MerchantInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户服务实现类
 */
@DS("merchant")
@Service
@Slf4j
public class MerchantInfoServiceImpl implements MerchantInfoService {

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Override
    @Cacheable(value = "merchant", key = "#page + '_' + #pageSize")
    public PageResult pageQuery(int page, int pageSize, String name, Integer status) {
        //1、构建分页条件
        Page<MerchantInfo> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, MerchantInfo::getName, name)
               .eq(status != null, MerchantInfo::getStatus, status)
               .orderByDesc(MerchantInfo::getCreateTime);

        //2、执行分页查询
        Page<MerchantInfo> result = merchantInfoMapper.selectPage(p, wrapper);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    @CacheEvict(value = "merchant", allEntries = true)
    public void audit(Long id, Integer status, String auditReason) {
        //1、查询商户信息
        MerchantInfo merchant = merchantInfoMapper.selectById(id);
        if (merchant == null) {
            log.warn("商户不存在，merchantId: {}", id);
            return;
        }

        //2、更新审核状态
        merchant.setStatus(status);
        merchant.setAuditReason(auditReason);
        merchantInfoMapper.updateById(merchant);
        log.info("商户审核完成，merchantId: {}, status: {}, reason: {}", id, status, auditReason);
    }

    @Override
    @Transactional
    @CacheEvict(value = "merchant", allEntries = true)
    public void updateStatus(Long id, Integer status) {
        //1、更新营业状态
        MerchantInfo merchant = MerchantInfo.builder().id(id).status(status).build();
        merchantInfoMapper.updateById(merchant);
        log.info("更新商户营业状态，merchantId: {}, status: {}", id, status);
    }

    @Override
    public MerchantInfo getById(Long id) {
        //1、查询商户详情
        return merchantInfoMapper.selectById(id);
    }

    @Override
    public List<MerchantInfo> listOpen() {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getStatus, 1)
               .orderByDesc(MerchantInfo::getCreateTime);
        return merchantInfoMapper.selectList(wrapper);
    }

    @Override
    public MerchantInfo getByEmpId(Long empId) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getEmpId, empId);
        return merchantInfoMapper.selectOne(wrapper);
    }
}
