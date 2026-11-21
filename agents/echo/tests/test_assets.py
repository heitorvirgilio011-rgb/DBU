"""ECHO offline static checks. Run: python -m unittest discover -v -s agents/echo/tests"""
import json
import math
import unittest
from pathlib import Path
P=Path(__file__).resolve().parents[1]
class Assets(unittest.TestCase):
    def test_hair_dbuh1_contract(self):
        data=(P/"hair_codes.tsv").read_text("utf8").strip().splitlines()
        self.assertEqual(len(data),3)
        for line in data:
            name,code=line.split("\t")
            self.assertRegex(name,r"^[a-z_]+$")
            self.assertTrue(code.startswith("DBUH1:"))
            self.assertLessEqual(len(code),4096)
            strands=[[int(x) for x in s.split(",")] for s in code[6:].split(";")]
            self.assertLessEqual(len(strands),40)
            self.assertEqual(strands[0],[0,-92,0,91,-30,91,0,0,0])
            for x,y,z,w,h,d,p,q,r in strands:
                self.assertTrue(-160<=x<=160 and -320<=y<=240 and -160<=z<=160)
                self.assertTrue(2<=w<=160 and -240<=h<=240 and h!=0 and 2<=d<=160)
                self.assertTrue(all(-180<=v<=180 for v in (p,q,r)))
    def test_race_proposals(self):
        d=json.loads((P/"race_head_geometry.json").read_text("utf8"))
        self.assertFalse(d["runtime_loaded"])
        names={v["race_id"]:v["head_enum"] for v in d["variants"].values()}
        self.assertEqual(names,{"namekian":"ANTENNAE","frost_demon":"HORNS","majin":"TENDRIL","bio_android":"CARAPACE"})
        for item in d["variants"].values():
            for name,origin,size,pivot,rotation in item["boxes"]:
                self.assertTrue(all(v>0 for v in size))
                self.assertEqual(len(origin),len(size))
                self.assertEqual(len(origin),len(pivot))
                self.assertEqual(len(origin),len(rotation))
    def test_seven_valid_obj_meshes(self):
        models=list((P/"obj").glob("*.obj"))
        self.assertEqual(len(models),7)
        for path in models:
            vertices=[]
            faces=0
            for line in path.read_text("utf8").splitlines():
                if line.startswith("v "):
                    xyz=[float(x) for x in line.split()[1:]]
                    self.assertTrue(all(math.isfinite(v) and abs(v)<48 for v in xyz))
                    vertices.append(xyz)
                elif line.startswith("f "):
                    ids=[int(x) for x in line.split()[1:]]
                    self.assertEqual(len(ids),3)
                    self.assertTrue(all(1<=i<=len(vertices) for i in ids))
                    a,b,c=(vertices[i-1] for i in ids)
                    u=[b[k]-a[k] for k in range(3)]
                    v=[c[k]-a[k] for k in range(3)]
                    cross=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
                    self.assertGreater(sum(k*k for k in cross),1e-12)
                    faces+=1
            self.assertGreater(len(vertices),20)
            self.assertGreater(faces,30)
if __name__=="__main__":unittest.main()
