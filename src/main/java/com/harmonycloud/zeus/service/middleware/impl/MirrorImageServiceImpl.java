package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageInfo;
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
    public void insert(String clusterId, String namespace, MirrorImageDTO mirrorImageDTO) {
         BeanMirrorImage beanMirrorImage = new BeanMirrorImage();
        BeanUtils.copyProperties(mirrorImageDTO,beanMirrorImage);
        String address = mirrorImageDTO.getHostAddress() + ":" + mirrorImageDTO.getPort() + "/" + mirrorImageDTO.getProject();
        beanMirrorImage.setClusterId(clusterId);
        if (StringUtils.isNotEmpty(namespace)) {
            beanMirrorImage.setNamespace(namespace);
        }
        beanMirrorImage.setAddress(address);
        beanMirrorImage.setCreateTime(new Date());
        beanMirrorImageMapper.insert(beanMirrorImage);
    }

    @Override
    public PageInfo<MirrorImageDTO> listMirrorImages(String clusterId, String namespace, String keyword) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("namespace",namespace).or().eq("namespace","");
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
    public void update(String clusterId, String namespace, MirrorImageDTO mirrorImageDTO) {
        BeanMirrorImage beanMirrorImage = new BeanMirrorImage();
        BeanUtils.copyProperties(mirrorImageDTO,beanMirrorImage);
        String address = mirrorImageDTO.getHostAddress() + ":" + mirrorImageDTO.getPort() + "/" + mirrorImageDTO.getProject();
        beanMirrorImage.setClusterId(clusterId);
        beanMirrorImage.setNamespace(namespace);
        beanMirrorImage.setAddress(address);
        beanMirrorImage.setUpdateTime(new Date());
        beanMirrorImageMapper.updateById(beanMirrorImage);
    }

    @Override
    public void delete(String clusterId, String namespace, String id) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("namespace",namespace).eq("id",id);
        beanMirrorImageMapper.delete(wrapper);
    }

    @Override
    public MirrorImageDTO detailByClusterId(String clusterId, String namespace) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("namespace",namespace);
        List<BeanMirrorImage> beanMirrorImages = beanMirrorImageMapper.selectList(wrapper);
        MirrorImageDTO mirrorImageDTO = new MirrorImageDTO();
        if (!beanMirrorImages.isEmpty()) {
            BeanUtils.copyProperties(beanMirrorImages.get(0),mirrorImageDTO);
        }
        return mirrorImageDTO;
    }
}
