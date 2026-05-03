import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Switch } from "../ui/switch";
import { Settings, Save, Loader2 } from "lucide-react";
import type { GameSettings } from "../../lib/types";

interface GameSettingsPanelProps {
  current: GameSettings;
  maxPlayers: number;
  isHost: boolean;
  onSave: (settings: GameSettings) => Promise<void>;
}

export function GameSettingsPanel({ current, maxPlayers, isHost, onSave }: GameSettingsPanelProps) {
  const [draft, setDraft] = useState<GameSettings>(current);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Sync draft when host updates settings remotely
  useEffect(() => {
    setDraft(current);
  }, [current]);

  const update = <K extends keyof GameSettings>(key: K, value: GameSettings[K]) => {
    if (!isHost) return;
    setDraft((prev) => ({ ...prev, [key]: value }));
  };

  const dirty = JSON.stringify(draft) !== JSON.stringify(current);
  const maxMafia = Math.max(1, Math.floor((maxPlayers - 1) / 2));

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      await onSave(draft);
    } catch (e) {
      setError((e as Error).message || "Failed to save settings");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader className="py-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <Settings className="w-4 h-4" />
          Game Settings
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Mafia count */}
        <div className="space-y-1">
          <Label htmlFor="mafiaCount" className="text-sm">
            Mafia members
          </Label>
          <Input
            id="mafiaCount"
            type="number"
            min={1}
            max={maxMafia}
            value={draft.mafiaCount}
            onChange={(e) => update("mafiaCount", Math.max(1, Math.min(maxMafia, parseInt(e.target.value) || 1)))}
            disabled={!isHost}
            className="w-24"
          />
          <p className="text-xs text-muted-foreground">Max {maxMafia} for {maxPlayers}-player lobbies</p>
        </div>

        {/* Role toggles */}
        <div className="space-y-2">
          <Label className="text-sm">Special Roles</Label>
          <RoleToggle
            label="Sheriff"
            description="Investigates one player each night"
            checked={draft.includeSheriff}
            onChange={(v) => update("includeSheriff", v)}
            disabled={!isHost}
          />
          <RoleToggle
            label="Doctor"
            description="Heals one player each night"
            checked={draft.includeDoctor}
            onChange={(v) => update("includeDoctor", v)}
            disabled={!isHost}
          />
          <RoleToggle
            label="Jester"
            description="Wins if voted out"
            checked={draft.includeJester}
            onChange={(v) => update("includeJester", v)}
            disabled={!isHost}
          />
          <RoleToggle
            label="Mutilator"
            description="Mutes a player or revokes their vote"
            checked={draft.includeMutilator}
            onChange={(v) => update("includeMutilator", v)}
            disabled={!isHost}
          />
        </div>

        {/* Conditional: doctor self-save limit */}
        {draft.includeDoctor && (
          <div className="space-y-1">
            <Label htmlFor="doctorSelfSaveLimit" className="text-sm">
              Doctor self-save limit
            </Label>
            <Input
              id="doctorSelfSaveLimit"
              type="number"
              min={-1}
              value={draft.doctorSelfSaveLimit}
              onChange={(e) => update("doctorSelfSaveLimit", parseInt(e.target.value) || 0)}
              disabled={!isHost}
              className="w-24"
            />
            <p className="text-xs text-muted-foreground">-1 = unlimited</p>
          </div>
        )}

        {/* Conditional: sheriff investigation delay */}
        {draft.includeSheriff && (
          <div className="space-y-1">
            <Label htmlFor="sheriffInvestigationDelay" className="text-sm">
              Sheriff investigation delay (rounds)
            </Label>
            <Input
              id="sheriffInvestigationDelay"
              type="number"
              min={0}
              value={draft.sheriffInvestigationDelay}
              onChange={(e) =>
                update("sheriffInvestigationDelay", Math.max(0, parseInt(e.target.value) || 0))
              }
              disabled={!isHost}
              className="w-24"
            />
            <p className="text-xs text-muted-foreground">0 = can investigate from round 1</p>
          </div>
        )}

        {error && <p className="text-sm text-red-500">{error}</p>}

        {isHost && dirty && (
          <Button onClick={handleSave} disabled={saving} className="w-full">
            {saving ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-2" />
                Save Settings
              </>
            )}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

interface RoleToggleProps {
  label: string;
  description: string;
  checked: boolean;
  onChange: (v: boolean) => void;
  disabled: boolean;
}

function RoleToggle({ label, description, checked, onChange, disabled }: RoleToggleProps) {
  return (
    <div className="flex items-start justify-between gap-3 py-1">
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      <Switch checked={checked} onCheckedChange={onChange} disabled={disabled} />
    </div>
  );
}
