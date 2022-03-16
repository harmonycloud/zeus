package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.model.middleware.MirrorImageDTO;
import com.harmonycloud.zeus.bean.BeanMirrorImage;
import com.harmonycloud.zeus.dao.BeanMirrorImageMapper;
import com.harmonycloud.zeus.service.middleware.MirrorImageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/3/10 下午3:14
 */
@Slf4j
@Service
public class MirrorImageServiceImpl implements MirrorImageService {

    @Autowired
    private BeanMirrorImageMapper beanMirrorImageMapper;

    @Override
    public void insert(String clusterId, MirrorImageDTO mirrorImageDTO) {
        BeanMirrorImage beanMirrorImage = new BeanMirrorImage();
        BeanUtils.copyProperties(mirrorImageDTO,beanMirrorImage);
        String address = mirrorImageDTO.getHostAddress() + ":" + mirrorImageDTO.getPort() + "/" + mirrorImageDTO.getProject();
        beanMirrorImage.setClusterId(clusterId);
        beanMirrorImage.setAddress(address);
        beanMirrorImage.setCreateTime(new Date());
        beanMirrorImageMapper.insert(beanMirrorImage);
    }

    @Override
    public PageInfo<MirrorImageDTO> listMirrorImages(String clusterId, String keyword) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId);
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.and(queryWrapper -> {
                queryWrapper.like("address",keyword).or().like("project",keyword).or().like("description",keyword);
            });
        }
        wrapper.orderByDesc("create_time");
        List<BeanMirrorImage> beanMirrorImages = beanMirrorImageMapper.selectList(wrapper);
        PageInfo<MirrorImageDTO> mirrorImageDTOPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(beanMirrorImages),mirrorImageDTOPageInfo);
        return mirrorImageDTOPageInfo;
    }

    @Override
    public void update(String clusterId, MirrorImageDTO mirrorImageDTO) {
        BeanMirrorImage beanMirrorImage = new BeanMirrorImage();
        BeanUtils.copyProperties(mirrorImageDTO,beanMirrorImage);
        String address = mirrorImageDTO.getHostAddress() + ":" + mirrorImageDTO.getPort() + "/" + mirrorImageDTO.getProject();
        beanMirrorImage.setClusterId(clusterId);
        beanMirrorImage.setAddress(address);
        beanMirrorImage.setUpdateTime(new Date());
        beanMirrorImageMapper.updateById(beanMirrorImage);
    }

    @Override
    public void delete(String clusterId, String id) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("id",id);
        beanMirrorImageMapper.delete(wrapper);
    }

    @Override
    public MirrorImageDTO detailByClusterId(String clusterId) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("is_default", CommonConstant.NUM_ONE);
        List<BeanMirrorImage> beanMirrorImages = beanMirrorImageMapper.selectList(wrapper);
        MirrorImageDTO mirrorImageDTO = new MirrorImageDTO();
        if (!beanMirrorImages.isEmpty()) {
            BeanUtils.copyProperties(beanMirrorImages.get(0),mirrorImageDTO);
        }
        return mirrorImageDTO;
    }

    @Override
    public MirrorImageDTO detailById(Integer id) {
        MirrorImageDTO mirrorImageDTO = new MirrorImageDTO();
        BeanMirrorImage beanMirrorImage = beanMirrorImageMapper.selectById(id);
        BeanUtils.copyProperties(beanMirrorImage,mirrorImageDTO);
        return mirrorImageDTO;
    }

    @Override
    public void removeMirrorImage(String clusterId) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId);
        beanMirrorImageMapper.delete(wrapper);
    }
}
