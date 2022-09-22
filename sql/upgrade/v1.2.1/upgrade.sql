-- 2022.9.22 liyinlong
-- 修改镜像仓库地址长度
ALTER TABLE `image_repository` MODIFY `address` VARCHAR(200);
ALTER TABLE `image_repository` MODIFY `host_address` VARCHAR(200);
