package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.ImageRepositoryDTO;
import com.harmonycloud.zeus.bean.BeanImageRepository;
import com.harmonycloud.zeus.dao.BeanImageRepositoryMapper;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午3:14
 */
@Slf4j
@Service
public class ImageRepositoryServiceImpl implements ImageRepositoryService {

    @Autowired
    private BeanImageRepositoryMapper beanImageRepositoryMapper;

    @Override
    public void insert(String clusterId, ImageRepositoryDTO imageRepositoryDTO) {
        check(imageRepositoryDTO);
        BeanImageRepository beanImageRepository = new BeanImageRepository();
        BeanUtils.copyProperties(imageRepositoryDTO, beanImageRepository);
        String address = imageRepositoryDTO.getHostAddress() + ":" + imageRepositoryDTO.getPort() + "/" + imageRepositoryDTO.getProject();
        beanImageRepository.setClusterId(clusterId);
        beanImageRepository.setAddress(address);
        beanImageRepository.setCreateTime(new Date());
        beanImageRepositoryMapper.insert(beanImageRepository);
    }

    @Override
    public PageInfo<ImageRepositoryDTO> listImageRepository(String clusterId, String keyword) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId);
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.and(queryWrapper -> {
                queryWrapper.like("address",keyword).or().like("project",keyword).or().like("description",keyword);
            });
        }
        wrapper.orderByDesc("create_time");
        List<BeanImageRepository> beanImageRepositories = beanImageRepositoryMapper.selectList(wrapper);
        PageInfo<ImageRepositoryDTO> ImageRepositoryDTOPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(beanImageRepositories),ImageRepositoryDTOPageInfo);
        return ImageRepositoryDTOPageInfo;
    }

    @Override
    public void update(String clusterId, ImageRepositoryDTO imageRepositoryDTO) {
        check(imageRepositoryDTO);
        BeanImageRepository beanImageRepository = new BeanImageRepository();
        BeanUtils.copyProperties(imageRepositoryDTO, beanImageRepository);
        String address = imageRepositoryDTO.getHostAddress() + ":" + imageRepositoryDTO.getPort() + "/" + imageRepositoryDTO.getProject();
        beanImageRepository.setClusterId(clusterId);
        beanImageRepository.setAddress(address);
        beanImageRepository.setUpdateTime(new Date());
        beanImageRepositoryMapper.updateById(beanImageRepository);
    }

    @Override
    public void delete(String clusterId, String id) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("id",id);
        beanImageRepositoryMapper.delete(wrapper);
    }

    @Override
    public ImageRepositoryDTO detailByClusterId(String clusterId) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("is_default", CommonConstant.NUM_ONE);
        List<BeanImageRepository> beanImageRepositories = beanImageRepositoryMapper.selectList(wrapper);
        ImageRepositoryDTO imageRepositoryDTO = new ImageRepositoryDTO();
        if (!beanImageRepositories.isEmpty()) {
            BeanUtils.copyProperties(beanImageRepositories.get(0), imageRepositoryDTO);
        }
        return imageRepositoryDTO;
    }

    @Override
    public ImageRepositoryDTO detailById(Integer id) {
        ImageRepositoryDTO imageRepositoryDTO = new ImageRepositoryDTO();
        BeanImageRepository beanImageRepository = beanImageRepositoryMapper.selectById(id);
        BeanUtils.copyProperties(beanImageRepository, imageRepositoryDTO);
        return imageRepositoryDTO;
    }

    @Override
    public void removeImageRepository(String clusterId) {
        QueryWrapper<BeanImageRepository> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId);
        beanImageRepositoryMapper.delete(wrapper);
    }

    /**
     * 中文字符校验
     */
    public void check(ImageRepositoryDTO imageRepositoryDTO){
        Pattern pattern = Pattern.compile("[\u4e00-\u9fa5]");
        if (pattern.matcher(imageRepositoryDTO.getProject()).find() || pattern.matcher(imageRepositoryDTO.getPort().toString()).find() || pattern.matcher(imageRepositoryDTO.getHostAddress()).find()){
            throw new BusinessException(ErrorMessage.DO_NOT_USE_CHINESE);
        }
    }
}
