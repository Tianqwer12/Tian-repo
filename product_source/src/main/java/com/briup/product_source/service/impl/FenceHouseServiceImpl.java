package com.briup.product_source.service.impl;

import com.briup.product_source.dao.ManagerFenceHouseMapper;
import com.briup.product_source.dao.ManagerHurdlesMapper;
import com.briup.product_source.dao.ext.ManagerFenceHouseExtMapper;
import com.briup.product_source.exception.ServiceException;
import com.briup.product_source.pojo.ManagerFenceHouse;
import com.briup.product_source.pojo.ext.ManagerFenceHouseExt;
import com.briup.product_source.result.ResultCode;
import com.briup.product_source.service.FenceHouseService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class FenceHouseServiceImpl implements FenceHouseService {

    @Autowired
    private ManagerFenceHouseMapper managerFenceHouseMapper;

    @Autowired
    private ManagerHurdlesMapper managerHurdlesMapper;

    @Autowired
    private ManagerFenceHouseExtMapper managerFenceHouseExtMapper;

    @Override
    public PageInfo<ManagerFenceHouse> findByPage(int pageNum, int pageSize, String fhName) {
        // PageHelper开启分页
        PageHelper.startPage(pageNum, pageSize);
        // 核心查询语句
        List<ManagerFenceHouse> managerFenceHouses = managerFenceHouseMapper.queryAllHouses(fhName);
        // 将查询的信息封装在PageInfo对象中，返回
        PageInfo<ManagerFenceHouse> pageInfo = new PageInfo<>(managerFenceHouses);
        return pageInfo;
    }

    @Override
    public void saveOrUpdate(ManagerFenceHouse managerFenceHouse) {
        // 参数校验
        String fhName = managerFenceHouse.getFhName(); //栏舍名称
        if (managerFenceHouse.getFhTime() == null || !StringUtils.hasText(fhName)) {
            throw new ServiceException(ResultCode.PARAM_IS_EMPTY);
        }

        int result; // sql语句执行后，返回受影响的行数result

        // 根据名称查询栏舍信息
        ManagerFenceHouse house = managerFenceHouseMapper.selectByFhName(fhName);
        String fhId = managerFenceHouse.getFhId(); // 栏圈编号
        if (StringUtils.hasText(fhId)) {
            // 有id->更新操作
            // 判断该条数据是否存在
            if (managerFenceHouseMapper.selectByPrimaryKey(fhId) == null) {
                throw new ServiceException(ResultCode.DATA_IS_EMPTY);
            }
            // 判断栏舍名称是否存在
            if (house != null && !house.getFhId().equals(fhId)) {
                throw new ServiceException(ResultCode.FENCE_HOUSE_IS_EXIST);
            }
            // 修改栏舍信息
            result = managerFenceHouseMapper.updateByPrimaryKey(managerFenceHouse);
        } else {
            // 无id->新增操作
            // 判断栏舍名称是否存在
            if (house != null) {
                throw new ServiceException(ResultCode.FENCE_HOUSE_IS_EXIST);
            }
            // 设置栏舍编号
            managerFenceHouse.setFhId(UUID.randomUUID().toString().replace("-", ""));
            // 添加栏舍信息
            result = managerFenceHouseMapper.insert(managerFenceHouse);
        }

        // 添加或者修改失败
        if (result == 0) {
            throw new ServiceException(ResultCode.FAIL);
        }
    }

    @Override
    public void removeById(String fhId) {
        if (managerFenceHouseMapper.selectByPrimaryKey(fhId) != null) {
            if (managerHurdlesMapper.selectCountByFhId(fhId) > 0) {
                // 栏舍下有栏圈信息
                throw new ServiceException(ResultCode.DATA_CAN_NOT_DELETE);
            }
            // 栏舍下没有栏圈信息，删除栏舍
            if (managerFenceHouseMapper.deleteByPrimaryKey(fhId) == 0) {
                // 删除失败
                throw new ServiceException(ResultCode.FAIL);
            }
        } else {
            // 数据不存在或者已删除
            throw new ServiceException(ResultCode.DATA_IS_EMPTY);
        }
    }

    @Override
    public void removeBatch(List<String> ids) {
        //能够被删除的栏舍id集合
        List<String> enableDeleteIds = new ArrayList<>();
        int emptyCount = 0; // 不存在的栏舍信息的条数
        int relativeCount = 0;// 关联栏圈信息的栏舍信息条数
        // 查询能够被删除的数据
        for (String id : ids) {
            if (managerFenceHouseMapper.selectByPrimaryKey(id) != null) {
                if (managerHurdlesMapper.selectCountByFhId(id) == 0) {
                    // 栏舍下没有栏圈信息
                    enableDeleteIds.add(id);
                }else {
                    relativeCount++;
                }
            }else {
                emptyCount++;
            }
        }

        // 要删除的栏舍信息都不存在
        if (emptyCount == ids.size()) {
            throw new ServiceException(ResultCode.DATA_IS_EMPTY);
        }
        // 要删除的栏舍信息都关联栏圈信息
        if (relativeCount == ids.size()) {
            throw new ServiceException(ResultCode.DATA_CAN_NOT_DELETE);
        }
        // 要删除的栏舍编号为空
        if (enableDeleteIds.isEmpty()) {
            throw new ServiceException(ResultCode.FAIL);
        }
        // 执行删除操作
        if (managerFenceHouseMapper.deleteBatchByIds(enableDeleteIds) == 0) {
            throw new ServiceException(ResultCode.FAIL);
        }
    }

    @Override
    public ManagerFenceHouseExt findById(String fhId) {
        ManagerFenceHouseExt managerFenceHouseExt = managerFenceHouseExtMapper.selectHouseAndHurdlesById(fhId);
        if (managerFenceHouseExt == null) {
            throw new ServiceException(ResultCode.FAIL);
        }
        return managerFenceHouseExt;
    }

    @Override
    public List<ManagerFenceHouse> findAll() {
        return managerFenceHouseMapper.selectAll();
    }
}
