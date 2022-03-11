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
        String address = mirrorImageDTO.getProtocol() + "://" + mirrorImageDTO.getHostAddress() + ":" + mirrorImageDTO.getPort();
        beanMirrorImage.setClusterId(clusterId);
        beanMirrorImage.setNamespace(namespace);
        beanMirrorImage.setAddress(address);
        beanMirrorImage.setCreateTime(new Date());
        beanMirrorImageMapper.insert(beanMirrorImage);
    }

    @Override
    public PageInfo<MirrorImageDTO> listMirrorImages(String clusterId, String namespace, String keyword) {
        QueryWrapper<BeanMirrorImage> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id",clusterId).eq("namespace",namespace);
        if (StringUtils.isNotEmpty(keyword)) {
            wrapper.and(queryWrapper -> {
                queryWrapper.like("address",keyword).or().like("project",keyword).or().like("description",keyword);
            });
        }
        List<BeanMirrorImage> beanMirrorImages = beanMirrorImageMapper.selectList(wrapper);
        PageInfo<MirrorImageDTO> mirrorImageDTOPageInfo = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(beanMirrorImages),mirrorImageDTOPageInfo);
        mirrorImageDTOPageInfo.getList().sort(
                (o1, o2) -> o1.getCreateTime() == null ? -1 : o2.getCreateTime() == null ? -1 : o2.getCreateTime().compareTo(o1.getCreateTime()));

        return mirrorImageDTOPageInfo;
    }

    @Override
    public void update(String clusterId, String namespace, MirrorImageDTO mirrorImageDTO) {
        BeanMirrorImage beanMirrorImage = new BeanMirrorImage();
        BeanUtils.copyProperties(mirrorImageDTO,beanMirrorImage);
        String address = mirrorImageDTO.getProtocol() + "://" + mirrorImageDTO.getHostAddress() + ":" + mirrorImageDTO.getPort();
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
}
