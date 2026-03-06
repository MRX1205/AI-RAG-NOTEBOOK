package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.lyhm.airag.mapper.ReportMapper;
import com.lyhm.airag.model.entity.Report;
import com.lyhm.airag.model.vo.ReportVO;
import com.lyhm.airag.service.ReportService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 报告服务实现类
 */
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report>
        implements ReportService {

    @Override
    public ReportVO getReportVO(Report report) {
        if (report == null)
            return null;
        ReportVO vo = new ReportVO();
        BeanUtil.copyProperties(report, vo);
        return vo;
    }

    @Override
    public List<ReportVO> getReportVOList(List<Report> reportList) {
        return reportList.stream().map(this::getReportVO).collect(Collectors.toList());
    }
}
