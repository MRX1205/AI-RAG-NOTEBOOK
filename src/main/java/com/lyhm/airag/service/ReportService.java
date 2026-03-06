package com.lyhm.airag.service;

import com.lyhm.airag.model.entity.Report;
import com.lyhm.airag.model.vo.ReportVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 报告服务接口
 */
public interface ReportService extends IService<Report> {

    ReportVO getReportVO(Report report);

    List<ReportVO> getReportVOList(List<Report> reportList);
}
