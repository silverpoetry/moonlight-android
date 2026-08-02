param(
    [string] $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

function New-ArgbBitmap {
    param(
        [int] $Width,
        [int] $Height
    )

    return [System.Drawing.Bitmap]::new(
        $Width,
        $Height,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
}

function Set-HighQualityRendering {
    param([System.Drawing.Graphics] $Graphics)

    $Graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $Graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $Graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $Graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
}

function Save-Png {
    param(
        [System.Drawing.Image] $Image,
        [string] $Path
    )

    $directory = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $Image.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
}

function Resize-Image {
    param(
        [System.Drawing.Image] $Source,
        [int] $Width,
        [int] $Height
    )

    $target = New-ArgbBitmap -Width $Width -Height $Height
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    try {
        Set-HighQualityRendering -Graphics $graphics
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.DrawImage($Source, 0, 0, $Width, $Height)
    }
    finally {
        $graphics.Dispose()
    }

    return $target
}

function New-RoundedRectanglePath {
    param(
        [System.Drawing.RectangleF] $Bounds,
        [float] $Radius
    )

    $diameter = $Radius * 2
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddArc($Bounds.Left, $Bounds.Top, $diameter, $diameter, 180, 90)
    $path.AddArc($Bounds.Right - $diameter, $Bounds.Top, $diameter, $diameter, 270, 90)
    $path.AddArc($Bounds.Right - $diameter, $Bounds.Bottom - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($Bounds.Left, $Bounds.Bottom - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-LegacyLauncherIcon {
    param(
        [System.Drawing.Image] $Source,
        [int] $Size
    )

    $target = New-ArgbBitmap -Width $Size -Height $Size
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    try {
        Set-HighQualityRendering -Graphics $graphics
        $graphics.Clear([System.Drawing.Color]::Transparent)

        $inset = [Math]::Max(1, [Math]::Round($Size * 0.04))
        $bounds = [System.Drawing.RectangleF]::new(
            $inset,
            $inset,
            $Size - (2 * $inset),
            $Size - (2 * $inset)
        )
        $path = New-RoundedRectanglePath -Bounds $bounds -Radius ($bounds.Width * 0.22)
        try {
            $graphics.SetClip($path)
            $graphics.DrawImage($Source, $bounds)
            $graphics.ResetClip()
        }
        finally {
            $path.Dispose()
        }
    }
    finally {
        $graphics.Dispose()
    }

    return $target
}

function Get-MattedForeground {
    param([System.Drawing.Bitmap] $Source)

    # The source artwork was composited on a vertical purple gradient. Solving
    # against the original white, yellow, and shadow colors retains anti-aliasing
    # while making the branded controller independent of the background layer.
    $white = [System.Drawing.Color]::FromArgb(255, 255, 255)
    $yellow = [System.Drawing.Color]::FromArgb(252, 204, 93)
    $matted = New-ArgbBitmap -Width $Source.Width -Height $Source.Height

    for ($y = 0; $y -lt $Source.Height; $y++) {
        $left = $Source.GetPixel(0, $y)
        $right = $Source.GetPixel($Source.Width - 1, $y)
        $backgroundR = ($left.R + $right.R) / 2.0
        $backgroundG = ($left.G + $right.G) / 2.0
        $backgroundB = ($left.B + $right.B) / 2.0

        for ($x = 0; $x -lt $Source.Width; $x++) {
            $sourceColor = $Source.GetPixel($x, $y)
            $deltaR = $sourceColor.R - $backgroundR
            $deltaG = $sourceColor.G - $backgroundG
            $deltaB = $sourceColor.B - $backgroundB
            if (($deltaR * $deltaR) + ($deltaG * $deltaG) + ($deltaB * $deltaB) -le 4) {
                continue
            }

            $bestError = [double]::PositiveInfinity
            $bestAlpha = 0.0
            $bestColor = $white
            $candidateColors = @($white)
            if ($x -ge 88 -and $x -le 138 -and $y -ge 102 -and $y -le 153) {
                $candidateColors += $yellow
            }
            foreach ($candidate in $candidateColors) {
                $candidateR = $candidate.R - $backgroundR
                $candidateG = $candidate.G - $backgroundG
                $candidateB = $candidate.B - $backgroundB
                $denominator = ($candidateR * $candidateR) +
                    ($candidateG * $candidateG) +
                    ($candidateB * $candidateB)
                if ($denominator -le 0) {
                    continue
                }

                $alpha = (($deltaR * $candidateR) +
                    ($deltaG * $candidateG) +
                    ($deltaB * $candidateB)) / $denominator
                $alpha = [Math]::Max(0.0, [Math]::Min(1.0, $alpha))
                $errorR = $deltaR - ($alpha * $candidateR)
                $errorG = $deltaG - ($alpha * $candidateG)
                $errorB = $deltaB - ($alpha * $candidateB)
                $error = ($errorR * $errorR) + ($errorG * $errorG) + ($errorB * $errorB)
                if ($error -lt $bestError) {
                    $bestError = $error
                    $bestAlpha = $alpha
                    $bestColor = $candidate
                }
            }

            $alphaByte = [int][Math]::Round($bestAlpha * 255)
            if ($alphaByte -gt 1) {
                $matted.SetPixel(
                    $x,
                    $y,
                    [System.Drawing.Color]::FromArgb(
                        $alphaByte,
                        $bestColor.R,
                        $bestColor.G,
                        $bestColor.B
                    )
                )
            }
        }
    }

    return $matted
}

function Get-AlphaBounds {
    param([System.Drawing.Bitmap] $Bitmap)

    $left = $Bitmap.Width
    $top = $Bitmap.Height
    $right = -1
    $bottom = -1
    for ($y = 0; $y -lt $Bitmap.Height; $y++) {
        for ($x = 0; $x -lt $Bitmap.Width; $x++) {
            if ($Bitmap.GetPixel($x, $y).A -le 1) {
                continue
            }
            $left = [Math]::Min($left, $x)
            $top = [Math]::Min($top, $y)
            $right = [Math]::Max($right, $x)
            $bottom = [Math]::Max($bottom, $y)
        }
    }

    if ($right -lt $left -or $bottom -lt $top) {
        throw 'Launcher foreground extraction produced an empty image.'
    }
    return [System.Drawing.Rectangle]::FromLTRB($left, $top, $right + 1, $bottom + 1)
}

function New-AdaptiveForeground {
    param(
        [System.Drawing.Bitmap] $MattedSource,
        [int] $Size
    )

    $bounds = Get-AlphaBounds -Bitmap $MattedSource
    $target = New-ArgbBitmap -Width $Size -Height $Size
    $graphics = [System.Drawing.Graphics]::FromImage($target)
    try {
        Set-HighQualityRendering -Graphics $graphics
        $graphics.Clear([System.Drawing.Color]::Transparent)

        # Android guarantees the centered 66x66 dp safe zone in a 108x108 dp layer.
        $targetWidth = $Size * (66.0 / 108.0)
        $targetHeight = $targetWidth * ($bounds.Height / [double] $bounds.Width)
        $targetBounds = [System.Drawing.RectangleF]::new(
            ($Size - $targetWidth) / 2.0,
            ($Size - $targetHeight) / 2.0,
            $targetWidth,
            $targetHeight
        )
        $graphics.DrawImage(
            $MattedSource,
            $targetBounds,
            $bounds,
            [System.Drawing.GraphicsUnit]::Pixel
        )
    }
    finally {
        $graphics.Dispose()
    }

    return $target
}

$launcherSourcePath = Join-Path $ProjectRoot 'app\src\main\artwork\ic_app_source.png'
$launcherSource = [System.Drawing.Bitmap]::new([string] $launcherSourcePath)
try {
    $densityScales = [ordered]@{
        'mdpi' = 1.0
        'hdpi' = 1.5
        'xhdpi' = 2.0
        'xxhdpi' = 3.0
        'xxxhdpi' = 4.0
    }

    foreach ($density in $densityScales.Keys) {
        $size = [int][Math]::Round(48 * $densityScales[$density])
        $legacy = New-LegacyLauncherIcon -Source $launcherSource -Size $size
        try {
            Save-Png -Image $legacy -Path (
                Join-Path $ProjectRoot "app\src\main\res\mipmap-$density\ic_app.png"
            )
        }
        finally {
            $legacy.Dispose()
        }
    }

    $mattedForeground = Get-MattedForeground -Source $launcherSource
    try {
        $adaptiveMaster = New-AdaptiveForeground -MattedSource $mattedForeground -Size 432
        try {
            foreach ($density in $densityScales.Keys) {
                $size = [int][Math]::Round(108 * $densityScales[$density])
                $foreground = Resize-Image -Source $adaptiveMaster -Width $size -Height $size
                try {
                    Save-Png -Image $foreground -Path (
                        Join-Path $ProjectRoot "app\src\main\res\mipmap-$density\ic_app_foreground.png"
                    )
                }
                finally {
                    $foreground.Dispose()
                }
            }
        }
        finally {
            $adaptiveMaster.Dispose()
        }
    }
    finally {
        $mattedForeground.Dispose()
    }
}
finally {
    $launcherSource.Dispose()
}

$densityImages = @(
    @{
        Source = 'app\src\main\res\drawable-xhdpi\atv_banner.png'
        Name = 'atv_banner.png'
    },
    @{
        Source = 'app\src\main\res\drawable-xhdpi\no_app_image.png'
        Name = 'no_app_image.png'
    }
)
foreach ($image in $densityImages) {
    $sourcePath = Join-Path $ProjectRoot $image.Source
    $source = [System.Drawing.Image]::FromFile($sourcePath)
    try {
        foreach ($density in $densityScales.Keys) {
            if ($density -eq 'xhdpi') {
                continue
            }
            $scale = $densityScales[$density] / 2.0
            $width = [int][Math]::Round($source.Width * $scale)
            $height = [int][Math]::Round($source.Height * $scale)
            $resized = Resize-Image -Source $source -Width $width -Height $height
            try {
                Save-Png -Image $resized -Path (
                    Join-Path $ProjectRoot "app\src\main\res\drawable-$density\$($image.Name)"
                )
            }
            finally {
                $resized.Dispose()
            }
        }
    }
    finally {
        $source.Dispose()
    }
}

Write-Output 'Generated launcher and density-specific image assets.'
