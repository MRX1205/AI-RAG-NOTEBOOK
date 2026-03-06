package com.lyhm.airag.mapper;

import com.lyhm.airag.model.entity.Notebook;
import com.mybatisflex.core.BaseMapper;

/**
 * 笔记本 Mapper 接口
 * <p>
 * 继承 MyBatis-Flex 的 BaseMapper，自动获得基础的 CRUD 方法：
 * - insert(entity)：插入一条记录
 * - deleteById(id)：按 ID 删除
 * - updateById(entity)：按 ID 更新
 * - selectOneById(id)：按 ID 查询
 * - selectAll()：查询所有
 * <p>
 * 如果需要自定义 SQL，可以在对应的 XML 文件中编写，
 * 路径为 resources/mapper/NotebookMapper.xml
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
public interface NotebookMapper extends BaseMapper<Notebook> {

}
