package rabbit.open.test;

import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import rabbit.open.test.entity.Organization;
import rabbit.open.test.entity.UUIDPolicyEntity;
import rabbit.open.test.entity.Zone;
import rabbit.open.test.service.OrganizationService;
import rabbit.open.test.service.UUIDEntityService;
import rabbit.open.test.service.ZoneService;

/**
 * <b>Description: 公共方法测试</b> 
 * <b>@author</b> 肖乾斌
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml" })
public class SpringDaoAdapterTest {

    @Autowired
    UUIDEntityService uues;

    @Autowired
    OrganizationService os;
    
    @Autowired
    ZoneService zs;

    @Test
    public void queryTest() {
        List<UUIDPolicyEntity> entities = uues.query(0, 10);
        entities.forEach(u -> System.out.println(u));
    }

    @Test
    public void delTest() {
        UUIDPolicyEntity ue = new UUIDPolicyEntity();
        ue.setName("llk");
        long before = uues.createQuery().count();
        uues.add(ue);
        TestCase.assertEquals(uues.createQuery().count(), 1 + before);
        uues.deleteByID(ue.getId());
        TestCase.assertEquals(uues.createQuery().count(), before);
    }

    @Test
    public void queryByCondition() {
        UUIDPolicyEntity ue = new UUIDPolicyEntity();
        ue.setName("llkx");
        uues.add(ue);
        List<UUIDPolicyEntity> entities = uues.query(ue, 0, 10);
        TestCase.assertEquals(entities.size(), 1);
        TestCase.assertEquals(entities.get(0).getId(), ue.getId());
    }

    @Test
    public void simpleCreateFieldsMappingQueryTest() {
        TestCase.assertEquals(uues.createFieldsMappingQuery(null).count(), uues.createQuery().count());
        UUIDPolicyEntity ue = new UUIDPolicyEntity();
        ue.setName("llkxs");
        uues.add(ue);
        NameMappedBean nmb = new NameMappedBean(ue.getId(), ue.getName(), 10);
        List<UUIDPolicyEntity> ues = uues.createFieldsMappingQuery(nmb).list();
        TestCase.assertEquals(1, ues.size());
        TestCase.assertEquals(ues.get(0).getId(), ue.getId());
        TestCase.assertEquals(ues.get(0).getName(), ue.getName());
    }

    /**
     * <b>Description createFieldsMappingQuery test</b>
     */
    @Test
    public void createFieldsMappingQueryTest() {
        Zone zone = new Zone();
        zone.setName("zzz");
        zs.add(zone);
        Organization o = new Organization("ORGxxx", "rtX");
        os.add(o);
        UUIDPolicyEntity ue = new UUIDPolicyEntity();
        ue.setName("llk");
        ue.setOrg(o);
        ue.setZone(zone);
        uues.add(ue);

        ZoneBean zb = new ZoneBean();
        zb.setId(zone.getId());
        zb.setName(zone.getName());
        
        NameMappedBean nmb = new NameMappedBean(ue.getId(), ue.getName(), zb, o);
        UUIDPolicyEntity e = uues.createFieldsMappingQuery(nmb)
                .fetch(Zone.class)
                .fetch(Organization.class)
                .unique();
        Zone z = e.getZone();
        Organization org = e.getOrg();
        TestCase.assertNotNull(e);
        TestCase.assertNotNull(org);
        TestCase.assertNotNull(z);

        TestCase.assertEquals(e.getId(), ue.getId());
        TestCase.assertEquals(z.getName(), zone.getName());
        TestCase.assertEquals(z.getId(), zone.getId());
        TestCase.assertEquals(org.getOrgCode(), o.getOrgCode());
        TestCase.assertEquals(org.getName(), o.getName());
        TestCase.assertEquals(org.getId(), o.getId());
    }

    @Test
    public void deleteByEntity() {
        UUIDPolicyEntity ue = new UUIDPolicyEntity();
        ue.setName("llk");
        uues.add(ue);
        TestCase.assertEquals(1, uues.delete(ue));
    }

    @Test
    public void updateByID() {
        UUIDPolicyEntity ue = new UUIDPolicyEntity();
        ue.setName("llk");
        uues.add(ue);
        TestCase.assertEquals(ue.getName(), uues.getByID(ue.getId()).getName());
        ue.setName("llkx");
        uues.updateByID(ue);
        TestCase.assertEquals(ue.getName(), uues.getByID(ue.getId()).getName());
    }

    public class ZoneBean {
        private Long id;

        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    
    public class NameMappedBean {

        private String id;

        private String name;

        // 年龄
        private Integer age;

        private Organization org;
        
        private ZoneBean zone;

        public NameMappedBean(String id, String name, ZoneBean z,
                Organization org) {
            super();
            this.id = id;
            this.name = name;
            this.org = org;
            this.zone = z;
        }

        public NameMappedBean(String id, String name, Integer age) {
            super();
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Organization getOrg() {
            return org;
        }

        public ZoneBean getZone() {
            return zone;
        }

        public void setZone(ZoneBean zb) {
            this.zone = zb;
        }

        
    }
}
