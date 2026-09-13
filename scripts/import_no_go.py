"""중원구청 관내도에서 뽑은 진입곤란 도로 GeoJSON을 no_go_areas 테이블로 임포트한다.

사용법:
    pip install -r scripts/requirements.txt
    python scripts/import_no_go.py                      # 로컬 MySQL
    python scripts/import_no_go.py --dry-run            # 접속 없이 검증만

접속 정보는 application.yml 의 local 프로파일과 같은 환경변수를 쓴다.
  LOCAL_DB_USERNAME / LOCAL_DB_PASSWORD (기본 root/root)
운영 DB에는 이 스크립트를 직접 돌리지 않는다.
"""
import argparse, json, os, sys

GEOJSON = os.path.join(os.path.dirname(__file__), "..", "data", "geo", "impassable_all.geojson")
STATIC_LAYER = 1          # 3층 하이브리드 중 정적 baseline
UNVERIFIED_NOTE = "지도상 미확인"   # 박종준 가이드의 표기를 그대로 쓴다
UNVERIFIED = "unverified"

def to_wkt(coords):
    """GeoJSON coordinates([lon,lat] 배열) -> WKT LINESTRING."""
    return "LINESTRING(" + ",".join(f"{lon} {lat}" for lon, lat in coords) + ")"

def rows_from(path):
    with open(path, encoding="utf-8") as fh:
        fc = json.load(fh)
    out = []
    for ft in fc["features"]:
        p = ft["properties"]
        c = ft["geometry"]["coordinates"]
        if ft["geometry"]["type"] != "LineString" or len(c) < 2:
            raise ValueError(f"{p.get('id')}: LineString이 아니거나 점이 부족하다")
        for lon, lat in c:
            # 성남 밖 좌표가 섞이면 축 순서를 뒤집었다는 뜻이다. 조용히 넘기지 않는다.
            if not (126.8 < lon < 127.4 and 37.2 < lat < 37.6):
                raise ValueError(f"{p['id']}: 성남 범위를 벗어난 좌표 ({lon}, {lat})")
        note = p.get("note", "")
        out.append({
            "ext_id": p["id"], "dong": p["dong"], "reason": p["reason"],
            "layer": STATIC_LAYER, "note": note,
            # 검증 상태는 note 와 분리된 컬럼이다(V2_3). note 는 나중에 현장 사유로 덮이므로
            # 라우팅 필터를 note 에 걸어두면 UPDATE 한 번에 뚫린다.
            "vstatus": UNVERIFIED if note == UNVERIFIED_NOTE else "ok",
            "source_pdf": p.get("source_pdf"), "source_page": p.get("source_page"),
            "med": p.get("georef_median_error_m"), "p90": p.get("georef_p90_error_m"),
            "wkt": to_wkt(c),
        })
    return out

# GeoJSON은 lon-lat 순서다. MySQL SRID 4326은 기본이 lat-lon 이므로 축 순서를 명시한다.
# V2_1 도 같은 규칙을 쓴다.
SQL = """
INSERT INTO no_go_areas
  (ext_id, dong, reason, layer, verification_status, note, source_pdf, source_page,
   georef_median_error_m, georef_p90_error_m, geom)
VALUES (%(ext_id)s, %(dong)s, %(reason)s, %(layer)s, %(vstatus)s, %(note)s, %(source_pdf)s,
        %(source_page)s, %(med)s, %(p90)s,
        ST_GeomFromText(%(wkt)s, 4326, 'axis-order=long-lat'))
ON DUPLICATE KEY UPDATE
  dong=VALUES(dong), reason=VALUES(reason), layer=VALUES(layer),
  verification_status=VALUES(verification_status), note=VALUES(note),
  source_pdf=VALUES(source_pdf), source_page=VALUES(source_page),
  georef_median_error_m=VALUES(georef_median_error_m),
  georef_p90_error_m=VALUES(georef_p90_error_m), geom=VALUES(geom)
"""

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--geojson", default=GEOJSON)
    ap.add_argument("--dry-run", action="store_true", help="DB 접속 없이 파일 검증만")
    ap.add_argument("--host", default=os.getenv("DB_HOST", "localhost"))
    ap.add_argument("--port", type=int, default=int(os.getenv("DB_PORT", "3306")))
    ap.add_argument("--db",   default=os.getenv("DB_NAME", "firedispatch"))
    a = ap.parse_args()

    rows = rows_from(a.geojson)
    dongs = sorted({r["dong"] for r in rows})
    print(f"세그먼트 {len(rows)}개, {len(dongs)}개 동")
    worst = max(rows, key=lambda r: r["med"] or 0)
    print(f"georef 최대 중간오차 {worst['med']}m ({worst['dong']})")
    unver = sum(1 for r in rows if r["vstatus"] == UNVERIFIED)
    print(f"verification_status=unverified {unver}건 (라우팅에서 제외되는 구간)")
    if a.dry_run:
        print("dry-run: DB에 쓰지 않았다"); return 0

    import pymysql
    conn = pymysql.connect(
        host=a.host, port=a.port, database=a.db,
        user=os.getenv("LOCAL_DB_USERNAME", os.getenv("DB_USERNAME", "root")),
        password=os.getenv("LOCAL_DB_PASSWORD", os.getenv("DB_PASSWORD", "root")),
        charset="utf8mb4", autocommit=False)
    try:
        with conn.cursor() as cur:
            cur.executemany(SQL, rows)
            n = cur.rowcount
        conn.commit()
        print(f"커밋 완료 (영향 행 {n})")
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*), COUNT(ext_id) FROM no_go_areas")
            total, imported = cur.fetchone()
        print(f"no_go_areas 총 {total}행 (임포트분 {imported}행)")
    except Exception:
        conn.rollback(); raise
    finally:
        conn.close()
    return 0

if __name__ == "__main__":
    sys.exit(main())
