with open("app/src/main/java/com/andev/absensiswaku/ui/presensi/PresensiFragment.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()
for i in range(588, 608):
    print(f"{i}: {repr(lines[i])}")
