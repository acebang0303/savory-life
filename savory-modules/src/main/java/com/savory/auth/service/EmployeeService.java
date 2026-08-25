package com.savory.auth.service;

import com.savory.auth.dto.EmployeeLoginDTO;
import com.savory.pojo.entity.Employee;

/**
 * 管理员服务接口
 */
public interface EmployeeService {

    /**
     * 管理员登录
     *
     * @param employeeLoginDTO 登录信息
     * @return 管理员实体
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 根据ID查询管理员
     */
    Employee getById(Long empId);

    /**
     * 分页查询员工
     */
    com.savory.common.result.PageResult pageQuery(int page, int pageSize, String name);
}
